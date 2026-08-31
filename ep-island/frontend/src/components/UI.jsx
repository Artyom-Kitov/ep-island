import {useCallback} from 'react';
import {STATUS_LABELS} from '../constants.js';

export function Brand({small = false}) {
    return <div className={small ? 'brand-row' : 'login-brand'}>
        <div className={`brand-mark${small ? ' small' : ''}`} aria-hidden="true">◇</div>
        <div><strong>EP Island</strong><span>{small ? 'операционный центр' : 'информационная система'}</span></div>
    </div>;
}

export function Badge({status, label = STATUS_LABELS[status] || status}) {
    const red = ['CANCELLED', 'OVERLOADED'].includes(status);
    const amber = ['PENDING', 'CREATED', 'WAITING', 'BUSY'].includes(status);
    const neutral = status === 'ARRIVED';
    const tone = red ? 'red' : amber ? 'amber' : neutral ? 'neutral' : 'green';
    return <span className={`badge ${tone}`}><i/>{label}</span>;
}

export function Notice({notice}) {
    if (!notice) return null;
    return <div className="notice" role="status" data-tone={notice.tone}>{notice.message}</div>;
}

export function Loading({label = 'Загрузка данных…'}) {
    return <div className="empty-state loading-state"><i/>{label}</div>;
}

export function Empty({children}) {
    return <div className="empty-state">{children}</div>;
}

export function RefreshButton({onClick, loading}) {
    return <button className="text-button" type="button" onClick={onClick} disabled={loading}>
        {loading ? 'Обновление…' : 'Обновить'}
    </button>;
}

export function SubmitButton({busy, children, busyLabel = 'Выполняется…', className = 'button primary'}) {
    return <button className={className} type="submit" disabled={busy}>
        {busy ? busyLabel : children}
    </button>;
}

export function useNotify(setNotice) {
    return useCallback((message, tone = 'success') => {
        setNotice({message, tone, id: Date.now()});
    }, [setNotice]);
}

export function number(value, maximumFractionDigits = 2) {
    return new Intl.NumberFormat('ru-RU', {maximumFractionDigits}).format(Number(value || 0));
}

export function dateTime(value) {
    return value
        ? new Intl.DateTimeFormat('ru-RU', {dateStyle: 'short', timeStyle: 'short'}).format(new Date(value))
        : '—';
}

export function loadTone(percent) {
    if (percent > 85) return 'danger';
    if (percent >= 60) return 'warning';
    return 'healthy';
}
