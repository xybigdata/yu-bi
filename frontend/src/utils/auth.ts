import {
  DEFAULT_AUTHORIZATION_TOKEN_EXPIRATION,
  StorageKeys,
} from 'globalConstants';
import { getDatartDateAfter } from 'app/utils/date';

let tokenExpiration = DEFAULT_AUTHORIZATION_TOKEN_EXPIRATION;

function getCookie(name: string) {
  const encodedName = encodeURIComponent(name);
  const cookies = document.cookie ? document.cookie.split('; ') : [];

  for (const cookie of cookies) {
    const [cookieName, ...cookieValueParts] = cookie.split('=');

    if (cookieName === encodedName) {
      return decodeURIComponent(cookieValueParts.join('='));
    }
  }

  return undefined;
}

function setCookie(name: string, value: string, expiresAt: Date) {
  document.cookie = [
    `${encodeURIComponent(name)}=${encodeURIComponent(value)}`,
    'path=/',
    `expires=${expiresAt.toUTCString()}`,
  ].join('; ');
}

function removeCookie(name: string) {
  document.cookie = [
    `${encodeURIComponent(name)}=`,
    'path=/',
    'expires=Thu, 01 Jan 1970 00:00:00 GMT',
  ].join('; ');
}

export function setTokenExpiration(expires: number) {
  tokenExpiration = expires;
}

export function getToken() {
  return getCookie(StorageKeys.AuthorizationToken);
}

export function setToken(token: string) {
  setCookie(
    StorageKeys.AuthorizationToken,
    token,
    getDatartDateAfter(tokenExpiration),
  );
}

export function removeToken() {
  removeCookie(StorageKeys.AuthorizationToken);
}
