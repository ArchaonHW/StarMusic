import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { ApiService } from './api.service';
import { environment } from '../../environments/environment';

describe('AuthService static storage recovery', () => {
  let values: Map<string, string>;
  const originalStaticData = environment.staticData;

  beforeEach(() => {
    values = new Map();
    spyOn(localStorage, 'getItem').and.callFake((key) => values.get(key) ?? null);
    spyOn(localStorage, 'setItem').and.callFake((key, value) => { values.set(key, value); });
    environment.staticData = true;
    TestBed.configureTestingModule({ providers: [{ provide: ApiService, useValue: {} }] });
  });

  afterEach(() => { environment.staticData = originalStaticData; });

  for (const stored of ['null', '{}', '[null, {}, {"member": {}}]', 'invalid-json']) {
    it(`recovers from malformed user and transaction storage: ${stored}`, () => {
      values.set('star-users', stored);
      values.set('star-tx', stored);
      let loggedIn = false;
      TestBed.inject(AuthService).login('starfan', 'starfan').subscribe((result) => {
        loggedIn = result.member.username === 'starfan';
      });
      expect(loggedIn).toBeTrue();
    });
  }
});
