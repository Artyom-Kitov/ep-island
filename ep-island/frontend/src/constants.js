export const PAGE_META = {
    dashboard: ['Большой Бредлам', 'Аналитический дашборд'],
    referrals: ['Полицейское управление', 'Электронные направления'],
    registration: ['Приёмный пункт', 'Регистрация прибывших'],
    zones: ['Развлекательный комплекс', 'Зоны и трансформация'],
    energy: ['Энергетическая станция', 'Стрижка и энергоучёт'],
    reports: ['Аналитический отдел', 'Отчётность']
};

export const ROLE_VIEWS = {
    OFFICER: 'referrals',
    REGISTRAR: 'registration',
    ZONE_OPERATOR: 'zones',
    ENGINEER: 'energy',
    ANALYST: 'reports',
    ADMIN: 'dashboard'
};

export const VIEW_ROLES = {
    dashboard: ['OFFICER', 'REGISTRAR', 'ZONE_OPERATOR', 'ENGINEER', 'ANALYST', 'ADMIN'],
    referrals: ['OFFICER', 'ADMIN'],
    registration: ['REGISTRAR', 'ADMIN'],
    zones: ['ZONE_OPERATOR', 'ADMIN'],
    energy: ['ENGINEER', 'ADMIN'],
    reports: ['ANALYST', 'ADMIN']
};

export const NAVIGATION = [
    ['dashboard', 'Дашборд'],
    ['referrals', 'Направления'],
    ['registration', 'Прибывшие'],
    ['zones', 'Зоны'],
    ['energy', 'Энергия'],
    ['reports', 'Отчёты']
];

export const STATUS_LABELS = {
    CREATED: 'Создано',
    HANDED_TO_CONVOY: 'Передано конвою',
    CANCELLED: 'Отменено',
    ARRIVED: 'Прибыл',
    ASSIGNED: 'В зоне',
    TRANSFORMED: 'Трансформирован',
    WAITING: 'Ожидает',
    COMPLETED: 'Завершено',
    DELIVERED: 'Передано',
    PENDING: 'Ожидает передачи'
};
