import {useCallback, useEffect, useState} from 'react';
import {api} from '../api.js';
import {Badge, Empty, Loading, RefreshButton, SubmitButton, dateTime, number} from '../components/UI.jsx';

export default function RegistrationView({user, notify}) {
    const [rows, setRows] = useState([]);
    const [referralId, setReferralId] = useState('');
    const [preview, setPreview] = useState(null);
    const [previewError, setPreviewError] = useState('');
    const [loading, setLoading] = useState(true);
    const [busy, setBusy] = useState(false);
    const canEdit = ['REGISTRAR', 'ADMIN'].includes(user.role);

    const load = useCallback(async () => {
        setLoading(true);
        try { setRows(await api('/api/residents')); }
        catch (error) { notify(error.message, 'error'); }
        finally { setLoading(false); }
    }, [notify]);

    useEffect(() => { load(); }, [load]);
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
            const row = await api('/api/residents', {method: 'POST', body: JSON.stringify({referralId: Number(referralId)})});
            setReferralId('');
            setPreview(null);
            notify(`Прибытие зарегистрировано. ID: ${row.id}`);
            await load();
        } catch (error) { notify(error.message, 'error'); }
        finally { setBusy(false); }
    }

    return <div className="content-grid form-and-list">
        <article className="card sticky-card">
            <p className="eyebrow">Приёмный пункт</p><h3>Регистрация прибытия</h3>
            <p className="section-lead">Введите числовой номер направления. Карточка будет заполнена автоматически.</p>
            <form className="stack-form compact" onSubmit={register}>
                <label>Номер направления<input value={referralId} onChange={event => setReferralId(event.target.value)} type="number" min="1" required placeholder="128" disabled={!canEdit}/></label>
                {!referralId && <div className="preview-box muted">Введите номер направления</div>}
                {preview && <div className="preview-box success"><div className="preview-title"><strong>Направление найдено</strong><Badge status={preview.status}/></div><dl><div><dt>ФИО</dt><dd>{preview.fullName}</dd></div><div><dt>Сумма долга</dt><dd>{number(preview.debtAmount)} ф.</dd></div></dl></div>}
                {previewError && <div className="preview-box error"><strong>Направление не найдено</strong><span>{previewError}</span></div>}
                <SubmitButton busy={busy}>Подтвердить регистрацию</SubmitButton>
            </form>
        </article>
        <article className="card">
            <div className="card-head"><div><p className="eyebrow">Остров</p><h3>Зарегистрированные коротышки</h3></div><RefreshButton onClick={load} loading={loading}/></div>
            {loading && !rows.length ? <Loading/> : <div className="table-wrap"><table><thead><tr><th>ID</th><th>ФИО</th><th>Направление</th><th>Статус</th><th>Прибыл</th></tr></thead><tbody>
                {rows.map(row => <tr key={row.id}><td><strong className="mono accent-text">{row.id}</strong></td><td>{row.fullName}</td><td>№ {row.referralId}</td><td><Badge status={row.status}/></td><td>{dateTime(row.arrivedAt)}</td></tr>)}
                {!rows.length && <tr><td colSpan="5"><Empty>Прибывших пока нет</Empty></td></tr>}
            </tbody></table></div>}
        </article>
    </div>;
}
