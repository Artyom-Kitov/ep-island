import {useCallback, useEffect, useState} from 'react';
import {api} from '../api.js';
import {Empty, Loading, RefreshButton, loadTone, number} from '../components/UI.jsx';

export default function DashboardView({notify, liveUpdate}) {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);

    const load = useCallback(async () => {
        setLoading(true);
        try {
            setData(await api('/api/analytics/dashboard'));
        } catch (error) {
            notify(error.message, 'error');
        } finally {
            setLoading(false);
        }
    }, [notify]);

    useEffect(() => { load(); }, [load]);
    useEffect(() => { if (liveUpdate) load(); }, [liveUpdate]);
    if (loading && !data) return <Loading/>;
    if (!data) return <Empty>Данные дашборда недоступны</Empty>;

    const totalResidents = data.arrived + data.assigned + data.transformed;
    const metrics = [
        ['Прибыло', totalResidents, `${data.assigned} распределено`, 'purple'],
        ['Трансформировано', data.transformed, 'готовы к стрижке', 'green'],
        ['Шерсть за период', `${number(data.woolKg)} кг`, 'принято станцией', 'amber'],
        ['Энерговыработка', `${number(data.energyKwh)} кВт·ч`, data.pendingDeliveries ? `${data.pendingDeliveries} в очереди` : 'данные переданы', 'green']
    ];

    return <>
        <div className="metric-grid">{metrics.map(([label, value, note, tone]) =>
            <article className={`metric ${tone}`} key={label}><span>{label}</span><strong>{value}</strong><small>{note}</small></article>)}</div>
        <div className="dashboard-grid">
            <article className="card">
                <div className="card-head"><div><p className="eyebrow">В реальном времени</p><h3>Загрузка развлекательных зон</h3></div><RefreshButton onClick={load} loading={loading}/></div>
                <div className="zone-overview">{data.zones.length ? data.zones.map(zone => <ZoneLoad key={zone.name} zone={zone}/>) : <Empty>Зоны пока не настроены</Empty>}</div>
            </article>
            <article className="card">
                <div className="card-head"><div><p className="eyebrow">Текущая смена</p><h3>Операционная сводка</h3></div></div>
                <div className="operations-summary">
                    <Summary label="Активные направления" value={data.referrals}/>
                    <Summary label="Ожидают распределения" value={data.arrived}/>
                    <Summary label="В процессе трансформации" value={data.assigned}/>
                    <Summary label="Передачи в очереди" value={data.pendingDeliveries} tone={data.pendingDeliveries ? 'warning-text' : 'success-text'}/>
                </div>
            </article>
        </div>
    </>;
}

function ZoneLoad({zone}) {
    const tone = loadTone(zone.loadPercent);
    const label = tone === 'danger' ? 'перегружена' : tone === 'warning' ? 'загружена' : 'свободна';
    return <div className="zone-load-row">
        <div className="zone-bar-head"><div><strong>Зона «{zone.name}»</strong><span>{zone.occupied} из {zone.capacity} мест</span></div><span className={`load-label ${tone}`}>{label}</span></div>
        <div className={`load-value ${tone}`}>{zone.loadPercent}%</div>
        <div className="bar"><i className={tone} style={{width: `${Math.min(zone.loadPercent, 100)}%`}}/></div>
    </div>;
}

function Summary({label, value, tone = ''}) {
    return <div className="summary-value"><span>{label}</span><strong className={tone}>{value}</strong></div>;
}
