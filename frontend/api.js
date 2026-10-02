'use strict';
window.MatchCatsAPI = (() => {
  let csrf;
  class APIError extends Error {
    constructor(status, code, fields = {}) { super(code); Object.assign(this, {status, code, fields}); }
  }
  async function request(path, {method = 'GET', body, binary = false, retry = true} = {}) {
    const changing = !['GET', 'HEAD'].includes(method);
    if (changing && !csrf) csrf = await request('/auth/csrf');
    const headers = {};
    if (changing) headers[csrf.headerName] = csrf.token;
    if (body && !(body instanceof FormData)) headers['Content-Type'] = 'application/json';
    let response;
    try {
      response = await fetch(path, {method, credentials:'same-origin', headers,
        body: body instanceof FormData ? body : body ? JSON.stringify(body) : undefined,
        signal: AbortSignal.timeout(20000)});
    } catch { throw new APIError(0, 'NETWORK'); }
    if (response.status === 403 && changing && retry) {
      csrf = await request('/auth/csrf');
      return request(path, {method, body, binary, retry:false});
    }
    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new APIError(response.status, error.code || 'HTTP_ERROR', error.fields);
    }
    if (response.status === 204) return null;
    if (binary) return response.blob();
    return response.json();
  }
  return {request, APIError, resetCSRF: () => {csrf = null;}};
})();
