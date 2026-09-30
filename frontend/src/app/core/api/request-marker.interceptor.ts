import { HttpInterceptorFn } from '@angular/common/http';

export const requestMarkerInterceptor: HttpInterceptorFn = (request, next) => {
  if (request.url.startsWith('/api/') && request.method !== 'GET') {
    return next(request.clone({ setHeaders: { 'X-Requested-With': 'wallet-dashboard' } }));
  }
  return next(request);
};
