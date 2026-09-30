import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { requestMarkerInterceptor } from './request-marker.interceptor';

describe('requestMarkerInterceptor', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(withInterceptors([requestMarkerInterceptor])), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('marks API mutations but not reads', () => {
    const client = TestBed.inject(HttpClient);
    client.post('/api/imports', {}).subscribe();
    expect(http.expectOne('/api/imports').request.headers.get('X-Requested-With')).toBe('wallet-dashboard');
    client.get('/api/accounts').subscribe();
    expect(http.expectOne('/api/accounts').request.headers.has('X-Requested-With')).toBeFalse();
  });
});
