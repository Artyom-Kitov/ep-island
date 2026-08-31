let unauthorizedHandler = () => {};

export function onUnauthorized(handler) {
    unauthorizedHandler = handler;
}

export async function api(path, options = {}) {
    const {headers = {}, ...requestOptions} = options;
    const response = await fetch(path, {
        credentials: 'same-origin',
        ...requestOptions,
        headers: {
            Accept: 'application/json',
            ...(requestOptions.body === undefined ? {} : {'Content-Type': 'application/json'}),
            ...headers
        }
    });

    if (response.status === 401 && !path.includes('/session/login')) {
        unauthorizedHandler();
        throw new Error('Сессия завершена. Войдите снова.');
    }
    if (!response.ok) {
        throw new Error(await errorMessage(response));
    }
    return response.status === 204 ? null : response.json();
}

export async function downloadCsv(path, filename) {
    const response = await fetch(path, {credentials: 'same-origin'});
    if (!response.ok) {
        throw new Error(await errorMessage(response));
    }
    const url = URL.createObjectURL(await response.blob());
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
}

async function errorMessage(response) {
    const fallback = `Ошибка ${response.status}`;
    try {
        const payload = await response.json();
        return payload.message || fallback;
    } catch (_) {
        return fallback;
    }
}

export function idempotencyKey() {
    return globalThis.crypto?.randomUUID?.()
        ?? `web-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}
