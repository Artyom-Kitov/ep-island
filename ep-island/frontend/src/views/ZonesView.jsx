import {useCallback, useEffect, useState} from 'react';
import {api} from '../api.js';
import {Badge, Empty, Loading, RefreshButton, SubmitButton, loadTone} from '../components/UI.jsx';

export default function ZonesView({user, notify}) {
    const [zones, setZones] = useState([]);
    const [residents, setResidents] = useState([]);
    const [assignments, setAssignments] = useState([]);
    const [residentId, setResidentId] = useState('');
    const [zoneId, setZoneId] = useState('');
    const [recommendation, setRecommendation] = useState(null);
    const [loading, setLoading] = useState(true);
    const [busy, setBusy] = useState(false);
    const canEdit = ['ZONE_OPERATOR', 'ADMIN'].includes(user.role);

    const load = useCallback(async () => {
        setLoading(true);
        try {
            const [zoneRows, residentRows, assignmentRows] = await Promise.all([
                api('/api/zones'), api('/api/residents'), api('/api/zones/assignments')
            ]);
            setZones(zoneRows); setResidents(residentRows); setAssignments(assignmentRows);
        } catch (error) { notify(error.message, 'error'); }
        finally { setLoading(false); }
    }, [notify]);

    useEffect(() => { load(); }, [load]);

    async function recommend() {
        if (!residentId) return notify('Выберите коротышку', 'error');
        try {
            const result = await api(`/api/zones/recommendation/${encodeURIComponent(residentId)}`);
            setRecommendation(result);
            setZoneId(String(result.zone.id));
        } catch (error) { notify(error.message, 'error'); }
    }

    async function assign(event) {
        event.preventDefault();
        setBusy(true);
        try {
            await api('/api/zones/assignments', {method: 'POST', body: JSON.stringify({residentId, zoneId: Number(zoneId)})});
            setResidentId(''); setZoneId(''); setRecommendation(null);
            notify('Назначение сохранено, загрузка зон обновлена');
            await load();
        } catch (error) { notify(error.message, 'error'); }
        finally { setBusy(false); }
    }

    const eligible = residents.filter(resident => ['ARRIVED', 'ASSIGNED'].includes(resident.status));
    const available = zones.filter(zone => zone.active && zone.occupied < zone.capacity);

    if (loading && !zones.length && !assignments.length) return <Loading/>;
    return <>
        <div className="section-toolbar"><p className="section-lead">Процент трансформации вручную фиксирует оператор зоны. При 100% система сама создаёт инженеру задание на стрижку.</p><RefreshButton onClick={load} loading={loading}/></div>
        <div className="zone-card-grid">{zones.map(zone => <ZoneCard key={zone.id} zone={zone}/>)}</div>
        <div className="content-grid form-and-list">
            <article className="card sticky-card">
                <p className="eyebrow">Распределение</p><h3>Назначить зону</h3>
                <form className="stack-form compact" onSubmit={assign}>
                    <label>Коротышка<select value={residentId} onChange={event => {setResidentId(event.target.value); setRecommendation(null);}} required disabled={!canEdit}><option value="">Выберите коротышку…</option>{eligible.map(resident => <option key={resident.id} value={resident.id}>{resident.fullName} · {resident.id}</option>)}</select></label>
                    <button type="button" className="button secondary" onClick={recommend} disabled={!canEdit}>Рассчитать рекомендацию</button>
                    {recommendation ? <div className="preview-box info"><div className="preview-title"><strong>Рекомендуемая зона: «{recommendation.zone.name}»</strong><Badge status="DELIVERED" label="оптимально"/></div><span>Оценка {recommendation.zone.score}. {recommendation.rationale}.</span></div> : <div className="preview-box muted">Выберите коротышку для расчёта</div>}
                    <label>Зона<select value={zoneId} onChange={event => setZoneId(event.target.value)} required disabled={!canEdit}><option value="">Выберите зону…</option>{available.map(zone => <option key={zone.id} value={zone.id}>{zone.name} · {zone.occupied}/{zone.capacity}</option>)}</select></label>
                    <SubmitButton busy={busy}>Подтвердить назначение</SubmitButton>
                </form>
            </article>
            <article className="card">
                <div className="card-head"><div><p className="eyebrow">Текущая работа</p><h3>Стадии трансформации</h3></div></div>
                <div className="assignment-list">{assignments.length ? assignments.map(assignment => <Assignment key={assignment.id} assignment={assignment} canEdit={canEdit} notify={notify} reload={load}/>) : <Empty>Активных назначений нет</Empty>}</div>
            </article>
        </div>
    </>;
}

function ZoneCard({zone}) {
    const load = Math.round(zone.occupied * 100 / zone.capacity);
    const tone = loadTone(load);
    const status = tone === 'danger' ? ['OVERLOADED', 'перегружена'] : tone === 'warning' ? ['BUSY', 'загружена'] : ['DELIVERED', 'свободна'];
    return <article className={`zone-card ${tone}`}><header><h3>Зона «{zone.name}»</h3><Badge status={status[0]} label={status[1]}/></header><strong className={`load-value ${tone}`}>{load}%</strong><div className="bar"><i className={tone} style={{width: `${load}%`}}/></div><small>{zone.occupied} из {zone.capacity} мест · коэффициент {zone.transformationCoefficient}</small></article>;
}

function Assignment({assignment, canEdit, notify, reload}) {
    const [percent, setPercent] = useState(assignment.transformationPercent);
    const [busy, setBusy] = useState(false);
    const canUpdate = canEdit && assignment.transformationPercent < 100;

    useEffect(() => setPercent(assignment.transformationPercent), [assignment.transformationPercent]);

    async function save() {
        setBusy(true);
        try {
            await api(`/api/zones/assignments/${assignment.id}/transformation`, {method: 'PATCH', body: JSON.stringify({percent: Number(percent)})});
            notify(Number(percent) === 100 ? 'Трансформация завершена, инженеру создано задание' : 'Стадия трансформации сохранена');
            await reload();
        } catch (error) { notify(error.message, 'error'); }
        finally { setBusy(false); }
    }

    return <div className="assignment"><div className="assignment-head"><div><strong>{assignment.fullName}</strong><small>{assignment.residentId} · зона «{assignment.zoneName}»</small></div><Badge status={assignment.transformationPercent === 100 ? 'COMPLETED' : 'ASSIGNED'}/></div><div className="progress-control"><div className="progress-copy"><span>Трансформация</span><strong>{assignment.transformationPercent}%</strong></div><div className="bar"><i className="healthy" style={{width: `${assignment.transformationPercent}%`}}/></div><div className="progress-actions"><input type="number" min={assignment.transformationPercent} max="100" value={percent} onChange={event => setPercent(event.target.value)} disabled={!canUpdate || busy}/><button type="button" className="button secondary" onClick={save} disabled={!canUpdate || busy}>{canUpdate ? busy ? 'Сохранение…' : 'Сохранить' : 'Завершено'}</button></div></div></div>;
}
