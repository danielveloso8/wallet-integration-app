import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { appRoutes } from './app.routes';
import { requestMarkerInterceptor } from './core/api/request-marker.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [provideRouter(appRoutes), provideHttpClient(withInterceptors([requestMarkerInterceptor]))]
};
