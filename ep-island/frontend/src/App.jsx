import {useEffect, useState} from 'react';
import {api, onUnauthorized} from './api.js';
import {NAVIGATION, PAGE_META, ROLE_VIEWS, VIEW_ROLES} from './constants.js';
import {Brand, Notice, useNotify} from './components/UI.jsx';
import DashboardView from './views/DashboardView.jsx';
import ReferralsView from './views/ReferralsView.jsx';
import RegistrationView from './views/RegistrationView.jsx';
import ZonesView from './views/ZonesView.jsx';
import EnergyView from './views/EnergyView.jsx';
import ReportsView from './views/ReportsView.jsx';

const VIEW_COMPONENTS = {
    dashboard: DashboardView,
    referrals: ReferralsView,
    registration: RegistrationView,
    zones: ZonesView,
    energy: EnergyView,
    reports: ReportsView
};

export default function App() {
    const [accounts, setAccounts] = useState([]);
    const [user, setUser] = useState(null);
    const [booting, setBooting] = useState(true);
    const [view, setView] = useState('dashboard');
    const [notice, setNotice] = useState(null);
    const [liveUpdate, setLiveUpdate] = useState(null);
    const notify = useNotify(setNotice);

    useEffect(() => {
        onUnauthorized(() => setUser(null));
        Promise.all([
            api('/api/session/demo-accounts'),
            api('/api/session').catch(() => null)
        ]).then(([demoAccounts, currentUser]) => {
            setAccounts(demoAccounts);
            if (currentUser) {
                setUser(currentUser);
                setView(ROLE_VIEWS[currentUser.role] || 'dashboard');
            }
        }).catch(error => notify(error.message, 'error'))
            .finally(() => setBooting(false));
    }, []);

    useEffect(() => {
        if (!notice) return undefined;
        const timeout = setTimeout(() => setNotice(null), 4800);
        return () => clearTimeout(timeout);
    }, [notice]);

    useEffect(() => {
        if (!user) return undefined;
        const events = new EventSource('/api/events');
        events.onmessage = event => {
            try {
                setLiveUpdate(JSON.parse(event.data));
            } catch (_) {
                // Heartbeats and malformed events do not affect application state.
            }
        };
        return () => events.close();
    }, [user]);

    if (booting) {
        return <div className="login-screen"><div className="boot-card"><div className="brand-mark">◇</div><span>Запуск EP Island…</span></div></div>;
    }

    if (!user) {
        return <><Login accounts={accounts} onLogin={loggedIn => {
            setUser(loggedIn);
            setView(ROLE_VIEWS[loggedIn.role] || 'dashboard');
        }} notify={notify}/><Notice notice={notice}/></>;
    }

    const CurrentView = VIEW_COMPONENTS[view] || DashboardView;
    const [kicker, title] = PAGE_META[view];
    const roleTitle = accounts.find(account => account.role === user.role)?.roleTitle || user.role;

    async function logout() {
        try {
            await api('/api/session', {method: 'DELETE'});
        } finally {
            setUser(null);
        }
    }

    return <div className="app-shell">
        <header className="app-header">
            <Brand small/>
            <nav aria-label="Основная навигация">
                {NAVIGATION.filter(([name]) => VIEW_ROLES[name].includes(user.role)).map(([name, label]) =>
                    <button key={name} type="button" className={`nav-item${view === name ? ' active' : ''}`}
                            onClick={() => setView(name)}>{label}</button>)}
            </nav>
            <div className="user-menu">
                <div className="avatar">{user.displayName.slice(0, 1).toUpperCase()}</div>
                <div><strong>{user.displayName}</strong><span>{roleTitle}</span></div>
                <button className="icon-button" type="button" title="Выйти" aria-label="Выйти" onClick={logout}>↪</button>
            </div>
        </header>
        <main className="main">
            <header className="page-header">
                <div><p className="eyebrow">{kicker}</p><h2>{title}</h2></div>
            </header>
            <CurrentView user={user} notify={notify} liveUpdate={liveUpdate}/>
        </main>
        <Notice notice={notice}/>
    </div>;
}

function Login({accounts, onLogin, notify}) {
    const [accountIndex, setAccountIndex] = useState(0);
    const selected = accounts[accountIndex];
    const [username, setUsername] = useState(selected?.username || 'admin');
    const [password, setPassword] = useState(selected?.password || 'admin');
    const [busy, setBusy] = useState(false);

    useEffect(() => {
        if (!selected) return;
        setUsername(selected.username);
        setPassword(selected.password);
    }, [selected]);

    async function submit(event) {
        event.preventDefault();
        setBusy(true);
        try {
            onLogin(await api('/api/session/login', {
                method: 'POST',
                body: JSON.stringify({username, password})
            }));
        } catch (error) {
            notify(error.message, 'error');
        } finally {
            setBusy(false);
        }
    }

    return <div className="login-screen">
        <section className="login-panel">
            <Brand/>
            <p className="eyebrow">Большой Бредлам</p>
            <h1>Вход в систему</h1>
            <p className="login-lead">Управление направлениями, размещением и энерговыработкой Острова Дураков.</p>
            <form className="stack-form" onSubmit={submit}>
                <label>Рабочий профиль
                    <select value={accountIndex} onChange={event => setAccountIndex(Number(event.target.value))} required>
                        {accounts.map((account, index) => <option key={account.username} value={index}>
                            {account.roleTitle} · {account.displayName}
                        </option>)}
                    </select>
                </label>
                <label>Логин<input value={username} onChange={event => setUsername(event.target.value)} autoComplete="username" required/></label>
                <label>Пароль<input value={password} onChange={event => setPassword(event.target.value)} type="password" autoComplete="current-password" required/></label>
                <button className="button primary wide" type="submit" disabled={busy}>{busy ? 'Вход…' : 'Войти в систему'}</button>
            </form>
            <p className="form-hint">Профиль определяет доступные разделы и операции.</p>
        </section>
    </div>;
}
