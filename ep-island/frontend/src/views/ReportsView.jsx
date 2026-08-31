import {useEffect, useState} from 'react';
import {api, downloadCsv} from '../api.js';
import {Empty, SubmitButton, number} from '../components/UI.jsx';

function defaultFilters() {
    const to = new Date();
    const from = new Date(to);
    from.setDate(to.getDate() - 30);
    return {from: from.toISOString().slice(0, 10), to: to.toISOString().slice(0, 10), zoneId: '', stage: '', referralStatus: ''};
}

export default function ReportsView({notify}) {
    const [zones, setZones] = useState([]);
    const [filters, setFilters] = useState(defaultFilters);
    const [rows, setRows] = useState([]);
    const [query, setQuery] = useState('');
    const [built, setBuilt] = useState(false);
    const [busy, setBusy] = useState(false);

    useEffect(() => { api('/api/zones').then(setZones).catch(error => notify(error.message, 'error')); }, [notify]);

    function field(name) {
        return {value: filters[name], onChange: event => setFilters(current => ({...current, [name]: event.target.value}))};
    }

    function params() {
        const result = new URLSearchParams();
        Object.entries(filters).forEach(([key, value]) => value && result.set(key, value));
        return result.toString();
    }

    async function build(event) {
        event.preventDefault(); setBusy(true);
        const nextQuery = params();
        try {
            const report = await api(`/api/analytics/report?${nextQuery}`);
            setRows(report); setQuery(nextQuery); setBuilt(true);
            notify(`Отчёт построен: ${report.length} строк`);
        } catch (error) { notify(error.message, 'error'); }
        finally { setBusy(false); }
    }

    async function exportReport() {
        try { await downloadCsv(`/api/analytics/report.csv?${query || params()}`, 'ep-island-report.csv'); notify('CSV-файл сформирован'); }
        catch (error) { notify(error.message, 'error'); }
    }

    const completed = rows.filter(row => row.transformationPercent === 100).length;
    const wool = rows.reduce((sum, row) => sum + Number(row.woolKg || 0), 0);
    const energy = rows.reduce((sum, row) => sum + Number(row.predictedEnergyKwh || 0), 0);

    return <div className="reports-layout">
        <aside className="card filters-card">
            <p className="eyebrow">Параметры выборки</p><h3>Фильтры</h3>
            <form className="stack-form compact" onSubmit={build}>
                <div className="form-row"><label>С<input {...field('from')} type="date"/></label><label>По<input {...field('to')} type="date"/></label></div>
                <label>Зона<select {...field('zoneId')}><option value="">Все зоны</option>{zones.map(zone => <option key={zone.id} value={zone.id}>{zone.name}</option>)}</select></label>
                <label>Стадия<select {...field('stage')}><option value="">Все</option><option value="INITIAL">Начальная</option><option value="INTERMEDIATE">Промежуточная</option><option value="COMPLETED">Завершённая</option></select></label>
                <label>Статус направления<select {...field('referralStatus')}><option value="">Все</option><option value="CREATED">Создано</option><option value="HANDED_TO_CONVOY">Передано конвою</option></select></label>
                <SubmitButton busy={busy}>Построить отчёт</SubmitButton>
            </form>
        </aside>
        <article className="card report-card">
            <div className="card-head"><div><p className="eyebrow">Результат</p><h3>Трансформация и энерговыработка</h3></div><button type="button" className="button secondary" onClick={exportReport} disabled={!rows.length}>Экспорт CSV</button></div>
            {!!rows.length && <div className="report-summary"><Summary label="В выборке" value={rows.length}/><Summary label="Трансформировано" value={completed} tone="success-text"/><Summary label="Шерсть" value={`${number(wool)} кг`}/><Summary label="Прогноз энергии" value={`${number(energy)} кВт·ч`} tone="success-text"/></div>}
            {!!rows.length && <div className="table-wrap report-table"><table><thead><tr><th>ID</th><th>ФИО</th><th>Зона</th><th>Стадия</th><th>Трансф.</th><th>Шерсть</th><th>Энергия</th><th>Дата</th></tr></thead><tbody>{rows.map(row => <tr key={row.residentId}><td><strong className="mono accent-text">{row.residentId}</strong></td><td>{row.fullName}</td><td>{row.zoneName || '—'}</td><td><span className={`stage${row.transformationPercent === 100 ? ' complete' : ''}`}>{row.transformationPercent === 100 ? 'трансформирован' : 'в процессе'}</span></td><td>{row.transformationPercent}%</td><td>{row.woolKg == null ? '—' : `${number(row.woolKg)} кг`}</td><td>{row.predictedEnergyKwh == null ? '—' : `${number(row.predictedEnergyKwh)} кВт·ч`}</td><td>{row.arrivedDate}</td></tr>)}</tbody></table></div>}
            {!rows.length && <Empty>{built ? 'По выбранным параметрам данных нет' : 'Задайте параметры и постройте отчёт'}</Empty>}
        </article>
    </div>;
}

function Summary({label, value, tone = ''}) {
    return <div><span>{label}</span><strong className={tone}>{value}</strong></div>;
}
