window.NUMBER_RECO_RUNTIME_CONFIG = window.NUMBER_RECO_RUNTIME_CONFIG || {
    // 留空时自动判断环境：本地开发走本机 8080，线上站点走 Railway。
    apiBaseUrl: ''
};

(function (global) {
    var STORAGE_KEY = 'numberReco.apiBaseUrl';
    var PRODUCTION_API_BASE_URL = 'https://number-recognition-system-production.up.railway.app';

    function normalizeBaseUrl(value) {
        var normalized = String(value || '').trim();
        normalized = normalized.replace(/\/+$/, '');
        return normalized;
    }

    function isLocalHostname(hostname) {
        var normalized = String(hostname || '').trim().toLowerCase();
        return normalized === 'localhost' ||
            normalized === '127.0.0.1' ||
            normalized === '0.0.0.0' ||
            normalized === '::1' ||
            /^10\./.test(normalized) ||
            /^192\.168\./.test(normalized) ||
            /^172\.(1[6-9]|2\d|3[0-1])\./.test(normalized);
    }

    function getLocalApiBaseUrl() {
        var hostname = global.location && global.location.hostname ? global.location.hostname : 'localhost';
        if (hostname === '0.0.0.0' || hostname === '::1') {
            hostname = 'localhost';
        }
        return 'http://' + hostname + ':8080';
    }

    function getDefaultApiBaseUrl() {
        var configured = normalizeBaseUrl(global.NUMBER_RECO_RUNTIME_CONFIG.apiBaseUrl);
        if (configured) {
            return configured;
        }

        var hostname = global.location && global.location.hostname ? global.location.hostname : '';
        if (isLocalHostname(hostname)) {
            return getLocalApiBaseUrl();
        }

        return PRODUCTION_API_BASE_URL;
    }

    function getStoredApiBaseUrl() {
        try {
            return global.localStorage.getItem(STORAGE_KEY) || '';
        } catch (error) {
            return '';
        }
    }

    function setStoredApiBaseUrl(value) {
        try {
            if (value) {
                global.localStorage.setItem(STORAGE_KEY, value);
            } else {
                global.localStorage.removeItem(STORAGE_KEY);
            }
        } catch (error) {
            // localStorage 不可用时忽略，继续使用默认配置
        }
    }

    function isAbsoluteUrl(value) {
        return /^https?:\/\//i.test(String(value || ''));
    }

    function ensureLeadingSlash(path) {
        if (!path) {
            return '/';
        }
        return path.charAt(0) === '/' ? path : '/' + path;
    }

    function getApiBaseUrl() {
        var stored = getStoredApiBaseUrl();
        return normalizeBaseUrl(stored) || getDefaultApiBaseUrl();
    }

    function setApiBaseUrl(value) {
        var normalized = normalizeBaseUrl(value);
        setStoredApiBaseUrl(normalized);
        return normalized || getDefaultApiBaseUrl();
    }

    function resetApiBaseUrl() {
        setStoredApiBaseUrl('');
        return getApiBaseUrl();
    }

    function toApiUrl(path) {
        if (isAbsoluteUrl(path)) {
            return path;
        }
        return getApiBaseUrl() + ensureLeadingSlash(path);
    }

    function toAssetUrl(path) {
        if (isAbsoluteUrl(path)) {
            return path;
        }
        return getApiBaseUrl() + ensureLeadingSlash(path);
    }

    global.NUMBER_RECO_RUNTIME_CONFIG.apiBaseUrl = getDefaultApiBaseUrl();
    global.NumberRecoRuntime = {
        getApiBaseUrl: getApiBaseUrl,
        setApiBaseUrl: setApiBaseUrl,
        resetApiBaseUrl: resetApiBaseUrl,
        toApiUrl: toApiUrl,
        toAssetUrl: toAssetUrl
    };
})(window);
