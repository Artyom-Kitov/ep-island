import {useCallback, useEffect, useState} from 'react';
import {api} from '../api.js';
import {Badge, Empty, Loading, RefreshButton, SubmitButton, dateTime, number} from '../components/UI.jsx';

export default function RegistrationView({user, notify, liveUpdate}) {
    const [residents, setResidents] = useState([]);
    const [pendingReferrals, setPendingReferrals] = useState([]);
    const [referralId, setReferralId] = useState('');
    const [fullName, setFullName] = useState('');
    const [preview, setPreview] = useState(null);
    const [previewError, setPreviewError] = useState('');
    const [loadingResidents, setLoadingResidents] = useState(true);
    const [loadingReferrals, setLoadingReferrals] = useState(true);
    const [busy, setBusy] = useState(false);
    const canEdit = ['REGISTRAR', 'ADMIN'].includes(user.role);

    const loadResidents = useCallback(async () => {
        setLoadingResidents(true);
        try { setResidents(await api('/api/residents')); }
        catch (error) { notify(error.message, 'error'); }
        finally { setLoadingResidents(false); }
    }, [notify]);

    const searchReferrals = useCallback(async query => {
        const normalized = query.trim();
        if (normalized.length === 1) {
            setPendingReferrals([]);
            setLoadingReferrals(false);
            return;
        }
        setLoadingReferrals(true);
        try {
            setPendingReferrals(await api(
                `/api/referrals/search?fullName=${encodeURIComponent(normalized)}&limit=12`
            ));
        } catch (error) { notify(error.message, 'error'); }
        finally { setLoadingReferrals(false); }
    }, [notify]);

    useEffect(() => { loadResidents(); }, [loadResidents]);
    useEffect(() => {
        if (liveUpdate && ['ALL', 'REFERRALS', 'RESIDENTS'].includes(liveUpdate.scope)) loadResidents();
    }, [liveUpdate, loadResidents]);

    useEffect(() => {
        const timeout = setTimeout(() => searchReferrals(fullName), 250);
        return () => clearTimeout(timeout);
    }, [fullName, searchReferrals]);

    useEffect(() => {
        if (liveUpdate && ['ALL', 'REFERRALS', 'RESIDENTS'].includes(liveUpdate.scope)) {
            searchReferrals(fullName);
        }
    }, [liveUpdate, fullName, searchReferrals]);

    useEffect(() => {
        setPreview(null);
        setPreviewError('');
        if (!referralId) return undefined;
        const timeout = setTimeout(async () => {
            try { setPreview(await api(`/api/referrals/${referralId}`)); }
            catch (error) { setPreviewError(error.message); }
        }, 250);
        return () => clearTimeout(timeout);
    }, [referralId]);

    async function register(event) {
        event.preventDefault();
        setBusy(true);
        try {
            const resident = await api('/api/residents', {
                method: 'POST',
                body: JSON.stringify({referralId: Number(referralId)})
            });
            setReferralId('');
            setPreview(null);
            notify(`Прибытие зарегистрировано. ID: ${resident.id}`);
            await Promise.all([loadResidents(), searchReferrals(fullName)]);
        } catch (error) { notify(error.message, 'error'); }
        finally { setBusy(false); }
    }

    function selectReferral(id) {
        setReferralId(String(id));
        window.scrollTo({top: 0, behavior: 'smooth'});
    }

    return <div className="content-grid form-and-list registration-layout">
        <article className="card sticky-card">
            <p className="eyebrow">Приёмный пункт</p><h3>Регистрация прибытия</h3>
            <p className="section-lead">Выберите актуальное направление справа или введите его номер.</p>
            <form className="stack-form compact" onSubmit={register}>
                <label>Номер направления<input value={referralId} onChange={event => setReferralId(event.target.value)} type="number" min="1" required placeholder="128" disabled={!canEdit}/></label>
                {!referralId && <div className="preview-box muted">Выберите или введите направление</div>}
                {preview && <div className="preview-box success"><div className="preview-title"><strong>Направление найдено</strong><Badge status={preview.status}/></div><dl><div><dt>ФИО</dt><dd>{preview.fullName}</dd></div><div><dt>Сумма долга</dt><dd>{number(preview.debtAmount)} ф.</dd></div></dl></div>}
                {previewError && <div className="preview-box error"><strong>Направление не найдено</strong><span>{previewError}</span></div>}
                <SubmitButton busy={busy}>Подтвердить регистрацию</SubmitButton>
            </form>
        </article>

        <div className="registration-side">
            <article className="card">
                <div className="card-head"><div><p className="eyebrow">Ожидают прибытия</p><h3>Актуальные направления</h3></div><RefreshButton onClick={() => searchReferrals(fullName)} loading={loadingReferrals}/></div>
                <label className="search-field">Поиск по ФИО<input value={fullName} onChange={event => setFullName(event.target.value)} placeholder="Начните вводить фамилию"/></label>
                <p className="search-summary">Показано: {pendingReferrals.length} · сначала новые</p>
                {loadingReferrals && !pendingReferrals.length ? <Loading label="Ищем направления…"/> : <div className="table-wrap compact-table"><table><thead><tr><th>№</th><th>ФИО</th><th>Статус</th><th/></tr></thead><tbody>
                    {pendingReferrals.map(referral => {
                        const ready = referral.status === 'HANDED_TO_CONVOY';
                        return <tr key={referral.id}><td><strong className="mono">№ {referral.id}</strong><small>{dateTime(referral.createdAt)}</small></td><td><strong>{referral.fullName}</strong><small>{number(referral.debtAmount)} ф.</small></td><td><Badge status={referral.status}/></td><td>{ready ? <button type="button" className="text-button" onClick={() => selectReferral(referral.id)}>Выбрать</button> : <small className="waiting-note">Ожидает конвой</small>}</td></tr>;
                    })}
                    {!pendingReferrals.length && <tr><td colSpan="4"><Empty>{fullName.trim().length === 1 ? 'Введите ещё один символ' : 'Подходящих направлений нет'}</Empty></td></tr>}
                </tbody></table></div>}
            </article>

            <article className="card">
                <div className="card-head"><div><p className="eyebrow">Остров</p><h3>Зарегистрированные коротышки</h3></div><RefreshButton onClick={loadResidents} loading={loadingResidents}/></div>
                {loadingResidents && !residents.length ? <Loading/> : <div className="table-wrap"><table><thead><tr><th>ID</th><th>ФИО</th><th>Направление</th><th>Статус</th><th>Прибыл</th></tr></thead><tbody>
                    {residents.map(row => <tr key={row.id}><td><strong className="mono accent-text">{row.id}</strong></td><td>{row.fullName}</td><td>№ {row.referralId}</td><td><Badge status={row.status}/></td><td>{dateTime(row.arrivedAt)}</td></tr>)}
                    {!residents.length && <tr><td colSpan="5"><Empty>Прибывших пока нет</Empty></td></tr>}
                </tbody></table></div>}
            </article>
        </div>
    </div>;
}
