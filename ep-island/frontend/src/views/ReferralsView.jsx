import {useCallback, useEffect, useState} from 'react';
import {api, idempotencyKey} from '../api.js';
import {Badge, Empty, Loading, RefreshButton, SubmitButton, dateTime, number} from '../components/UI.jsx';

const EMPTY_FORM = {fullName: '', birthDate: '', debtAmount: '', reason: '', documents: ''};

export default function ReferralsView({user, notify, liveUpdate}) {
    const [rows, setRows] = useState([]);
    const [form, setForm] = useState(EMPTY_FORM);
    const [registry, setRegistry] = useState(null);
    const [loading, setLoading] = useState(true);
    const [busy, setBusy] = useState(false);
    const canEdit = ['OFFICER', 'ADMIN'].includes(user.role);

    const load = useCallback(async () => {
        setLoading(true);
        try { setRows(await api('/api/referrals')); }
        catch (error) { notify(error.message, 'error'); }
        finally { setLoading(false); }
    }, [notify]);

    useEffect(() => { load(); }, [load]);
    useEffect(() => {
        if (liveUpdate && ['ALL', 'REFERRALS'].includes(liveUpdate.scope)) load();
    }, [liveUpdate, load]);

    function field(name) {
        return {value: form[name], onChange: event => setForm(current => ({...current, [name]: event.target.value}))};
    }

    async function searchRegistry() {
        if (!form.fullName.trim()) return notify('Введите ФИО должника', 'error');
        try {
            const result = await api(`/api/referrals/registry-search?fullName=${encodeURIComponent(form.fullName.trim())}`);
            setRegistry(result);
            if (result.found) setForm(current => ({...current, birthDate: result.birthDate || '', debtAmount: result.debtAmount, reason: result.reason, documents: result.documents}));
        } catch (error) { notify(error.message, 'error'); }
    }

    async function create(event) {
        event.preventDefault();
        setBusy(true);
        try {
            const created = await api('/api/referrals', {
                method: 'POST',
                headers: {'Idempotency-Key': idempotencyKey()},
                body: JSON.stringify({...form, birthDate: form.birthDate || null, debtAmount: Number(form.debtAmount)})
            });
            setForm(EMPTY_FORM);
            setRegistry(null);
            notify(`Направление № ${created.id} создано`);
            await load();
        } catch (error) { notify(error.message, 'error'); }
        finally { setBusy(false); }
    }

    async function handover(id) {
        try {
            await api(`/api/referrals/${id}/status`, {method: 'PATCH', body: JSON.stringify({status: 'HANDED_TO_CONVOY'})});
            notify('Направление передано конвою');
            await load();
        } catch (error) { notify(error.message, 'error'); }
    }

    async function remove(id) {
        if (!confirm('Удалить ошибочное направление?')) return;
        try {
            await api(`/api/referrals/${id}`, {method: 'DELETE'});
            notify('Направление удалено');
            await load();
        } catch (error) { notify(error.message, 'error'); }
    }

    return <div className="content-grid form-and-list">
        <article className="card sticky-card">
            <p className="eyebrow">Регистрация должника</p><h3>Новое направление</h3>
            <p className="section-lead">Найдите задержанного в реестре или заполните сведения вручную.</p>
            <form className="stack-form compact" onSubmit={create}>
                <label>ФИО задержанного<div className="input-action"><input {...field('fullName')} required maxLength="200" placeholder="Иванов Иван Иванович" disabled={!canEdit}/><button type="button" className="button secondary" onClick={searchRegistry} disabled={!canEdit}>Найти</button></div></label>
                {registry && <div className={`status-panel ${registry.found ? '' : 'warning'}`}><strong>{registry.found ? 'Запись найдена в реестре' : 'Совпадений не найдено'}</strong><span>{registry.found ? 'Поля заполнены автоматически.' : 'Введите сведения вручную.'}</span></div>}
                <div className="form-row"><label>Дата рождения<input {...field('birthDate')} type="date" disabled={!canEdit}/></label><label>Сумма долга, ф.<input {...field('debtAmount')} type="number" min="0" step="0.01" required disabled={!canEdit}/></label></div>
                <label>Основание<textarea {...field('reason')} rows="3" required maxLength="1000" disabled={!canEdit}/></label>
                <label>Документы<input {...field('documents')} maxLength="2000" placeholder="Номера и наименования документов" disabled={!canEdit}/></label>
                <SubmitButton busy={busy || !canEdit}>Создать направление</SubmitButton>
            </form>
        </article>
        <article className="card">
            <div className="card-head"><div><p className="eyebrow">Реестр</p><h3>Электронные направления</h3></div><RefreshButton onClick={load} loading={loading}/></div>
            {loading && !rows.length ? <Loading/> : <div className="table-wrap"><table><thead><tr><th>№</th><th>Должник</th><th>Долг</th><th>Статус</th><th/></tr></thead><tbody>
                {rows.map(row => <tr key={row.id}>
                    <td><strong className="mono">№ {row.id}</strong><small>{dateTime(row.createdAt)}</small></td>
                    <td><strong>{row.fullName}</strong><small>{row.reason}</small></td>
                    <td>{number(row.debtAmount)} ф.</td><td><Badge status={row.status}/></td>
                    <td><div className="row-actions">{canEdit && row.status === 'CREATED' && <><button type="button" onClick={() => handover(row.id)}>Передать</button><button type="button" className="danger" onClick={() => remove(row.id)}>Удалить</button></>}</div></td>
                </tr>)}
                {!rows.length && <tr><td colSpan="5"><Empty>Направлений пока нет</Empty></td></tr>}
            </tbody></table></div>}
        </article>
    </div>;
}
