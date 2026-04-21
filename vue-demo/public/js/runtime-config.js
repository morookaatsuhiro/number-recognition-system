window.NUMBER_RECO_RUNTIME_CONFIG = window.NUMBER_RECO_RUNTIME_CONFIG || {
    // 部署后只需要改这里，就能统一切换所有页面的后端地址。
    apiBaseUrl: 'http://localhost:8080'
};

(function (global) {
    var STORAGE_KEY = 'numberReco.apiBaseUrl';

    function normalizeBaseUrl(value) {
        var normalized = String(value || '').trim();
        normalized = normalized.replace(/\/+$/, '');
        return normalized || 'http://localhost:8080';
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
        var configured = global.NUMBER_RECO_RUNTIME_CONFIG.apiBaseUrl;
        var stored = getStoredApiBaseUrl();
        return normalizeBaseUrl(stored || configured);
    }

    function setApiBaseUrl(value) {
        var normalized = normalizeBaseUrl(value);
        setStoredApiBaseUrl(normalized);
        return normalized;
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

    global.NUMBER_RECO_RUNTIME_CONFIG.apiBaseUrl = normalizeBaseUrl(global.NUMBER_RECO_RUNTIME_CONFIG.apiBaseUrl);
    global.NumberRecoRuntime = {
        getApiBaseUrl: getApiBaseUrl,
        setApiBaseUrl: setApiBaseUrl,
        resetApiBaseUrl: resetApiBaseUrl,
        toApiUrl: toApiUrl,
        toAssetUrl: toAssetUrl
    };
})(window);
