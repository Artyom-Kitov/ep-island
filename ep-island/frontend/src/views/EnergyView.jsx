import {useCallback, useEffect, useState} from 'react';
import {api} from '../api.js';
import {Badge, Empty, Loading, RefreshButton, SubmitButton, dateTime, number} from '../components/UI.jsx';

export default function EnergyView({user, notify}) {
    const [shearings, setShearings] = useState([]);
    const [shifts, setShifts] = useState([]);
    const [shiftCode, setShiftCode] = useState('');
    const [actualKwh, setActualKwh] = useState('');
    const [loading, setLoading] = useState(true);
    const [busy, setBusy] = useState(false);
    const canEdit = ['ENGINEER', 'ADMIN'].includes(user.role);

    const load = useCallback(async () => {
        setLoading(true);
        try {
            const [shearingRows, shiftRows] = await Promise.all([api('/api/energy/shearings'), api('/api/energy/shifts')]);
            setShearings(shearingRows); setShifts(shiftRows);
        } catch (error) { notify(error.message, 'error'); }
        finally { setLoading(false); }
    }, [notify]);

    useEffect(() => { load(); }, [load]);

    async function createShift(event) {
        event.preventDefault();
        setBusy(true);
        try {
            const row = await api('/api/energy/shifts', {method: 'POST', body: JSON.stringify({shiftCode, actualKwh: Number(actualKwh)})});
            setShiftCode(''); setActualKwh('');
            notify(row.deliveryStatus === 'DELIVERED' ? 'Данные смены переданы в бухгалтерию' : 'Данные сохранены и поставлены в очередь', row.deliveryStatus === 'DELIVERED' ? 'success' : 'warning');
            await load();
        } catch (error) { notify(error.message, 'error'); }
        finally { setBusy(false); }
    }

    if (loading && !shearings.length && !shifts.length) return <Loading/>;
    return <div className="content-grid equal">
        <article className="card">
            <div className="card-head"><div><p className="eyebrow">Стрижка</p><h3>Задания инженеру</h3><p className="section-lead">Инженер вводит массу шерсти, а прогноз энергии рассчитывается автоматически.</p></div><RefreshButton onClick={load} loading={loading}/></div>
            <div className="assignment-list">{shearings.length ? shearings.map(row => <Shearing key={row.residentId} row={row} canEdit={canEdit} notify={notify} reload={load}/>) : <Empty>Заданий на стрижку нет</Empty>}</div>
        </article>
        <article className="card">
            <p className="eyebrow">Энерговыработка</p><h3>Закрытие смены</h3>
            <p className="section-lead">Инженер вручную фиксирует фактическую выработку. Передача в бухгалтерию и повторы при сбое выполняются автоматически.</p>
            <form className="stack-form compact" onSubmit={createShift}>
                <label>Идентификатор смены<input value={shiftCode} onChange={event => setShiftCode(event.target.value)} placeholder="SH-2026-08-30-A" required maxLength="80" disabled={!canEdit}/></label>
                <label>Фактическая выработка, кВт·ч<input value={actualKwh} onChange={event => setActualKwh(event.target.value)} type="number" min="0" step="0.1" required disabled={!canEdit}/></label>
                <SubmitButton busy={busy}>Зафиксировать и передать</SubmitButton>
            </form>
            <div className="divider"/><h4>Последние смены</h4>
            <div className="shift-list">{shifts.length ? shifts.slice(0, 8).map(row => <Shift key={row.id} row={row} canEdit={canEdit} notify={notify} reload={load}/>) : <Empty>Смен пока нет</Empty>}</div>
        </article>
    </div>;
}

function Shearing({row, canEdit, notify, reload}) {
    const [woolKg, setWoolKg] = useState(row.woolKg ?? '');
    const [busy, setBusy] = useState(false);
    async function submit(event) {
        event.preventDefault(); setBusy(true);
        try {
            await api(`/api/energy/shearings/${encodeURIComponent(row.residentId)}`, {method: 'PATCH', body: JSON.stringify({woolKg: Number(woolKg)})});
            notify('Стрижка учтена, прогноз энергии рассчитан'); await reload();
        } catch (error) { notify(error.message, 'error'); }
        finally { setBusy(false); }
    }
    return <div className="assignment"><div className="assignment-head"><div><strong>{row.fullName}</strong><small className="mono">{row.residentId}</small></div><Badge status={row.status}/></div><form className="inline-form" onSubmit={submit}><label>Получено шерсти, кг<input value={woolKg} onChange={event => setWoolKg(event.target.value)} type="number" min="0.1" step="0.1" required disabled={!canEdit}/></label><SubmitButton busy={busy} className="button secondary">{row.status === 'WAITING' ? 'Завершить' : 'Скорректировать'}</SubmitButton></form>{row.status === 'COMPLETED' && <div className="result-strip"><div><span>Шерсть</span><strong>{number(row.woolKg)} кг</strong></div><div><span>Прогноз энергии</span><strong>{number(row.predictedEnergyKwh)} кВт·ч</strong></div></div>}</div>;
}

function Shift({row, canEdit, notify, reload}) {
    const [value, setValue] = useState(row.actualKwh);
    const [busy, setBusy] = useState(false);
    async function submit(event) {
        event.preventDefault(); setBusy(true);
        try {
            const updated = await api(`/api/energy/shifts/${row.id}`, {method: 'PATCH', body: JSON.stringify({actualKwh: Number(value)})});
            notify(updated.deliveryStatus === 'DELIVERED' ? 'Корректировка сохранена и передана' : 'Корректировка сохранена; сработает автоповтор', updated.deliveryStatus === 'DELIVERED' ? 'success' : 'warning');
            await reload();
        } catch (error) { notify(error.message, 'error'); }
        finally { setBusy(false); }
    }
    return <div className="shift-row"><div><strong className="mono">{row.shiftCode}</strong><span>{dateTime(row.createdAt)}</span></div><div className="shift-value"><strong>{number(row.actualKwh)} кВт·ч</strong><Badge status={row.deliveryStatus}/></div><form className="shift-correction-form" onSubmit={submit}><input value={value} onChange={event => setValue(event.target.value)} type="number" min="0" step="0.1" aria-label="Скорректированная выработка" required disabled={!canEdit || busy}/><button className="text-button" type="submit" disabled={!canEdit || busy}>{busy ? 'Сохранение…' : 'Исправить'}</button></form>{row.deliveryStatus === 'PENDING' && <small className="warning-text">Автоповтор активен</small>}</div>;
}
