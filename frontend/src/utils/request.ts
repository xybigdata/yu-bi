/**
 * Datart
 *
 * Copyright 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { message } from 'antd';
import axios, { AxiosRequestConfig, AxiosResponse } from 'axios';
import { BASE_API_URL, PUBLIC_URL } from 'globalConstants';
import i18next from 'i18next';
import { APIResponse } from 'types';
import { getToken, removeToken, setToken } from './auth';

export type HeaderResponse<T> = [T, AxiosResponse['headers']];
type RequestExtra<T> = {
  onFulfilled?: (value: AxiosResponse<unknown>) => APIResponse<T>;
  onRejected?: (error: unknown) => unknown;
};
type ResponseError = {
  response?: {
    status?: number;
    data?: {
      message?: unknown;
    };
  };
};

export const instance = axios.create({
  baseURL: BASE_API_URL,
  validateStatus(status) {
    return status < 400;
  },
});

instance.interceptors.request.use(config => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = token;
  }
  return config;
});

instance.interceptors.response.use(response => {
  // refresh access token
  const token = response.headers.authorization;
  if (token) {
    setToken(token);
  }
  return response;
});

/**
 * New Http Request Util
 * Feature:
 *  1. Support customize onFulfilled and onRejected handler
 *  2. Support default backend service response error handler
 *  3. Support redux rejected action handler @see rejectedActionMessageHandler
 * @template T
 * @param {(string | AxiosRequestConfig)} url
 * @param {AxiosRequestConfig} [config]
 * @param {{
 *     onFulfilled?: (value: AxiosResponse<unknown>) => APIResponse<T>;
 *     onRejected?: (error: unknown) => unknown;
 *   }} [extra]
 * @return {*}  {Promise<APIResponse<T>>}
 */
export function request2<T>(
  url: string | AxiosRequestConfig,
  config?: AxiosRequestConfig,
  extra?: RequestExtra<T>,
): Promise<APIResponse<T>> {
  const defaultFulfilled = (response: AxiosResponse<unknown>) =>
    response.data as APIResponse<T>;
  const defaultRejected = (error: unknown): never => {
    throw standardErrorMessageTransformer(error);
  };
  const axiosPromise =
    typeof url === 'string' ? instance(url, config) : instance(url);
  return (axiosPromise as Promise<AxiosResponse<unknown>>)
    .then(extra?.onFulfilled || defaultFulfilled, unAuthorizationErrorHandler)
    .catch(extra?.onRejected || defaultRejected) as Promise<APIResponse<T>>;
}

export function requestWithHeader<T = unknown>(
  url: string | AxiosRequestConfig,
  config?: AxiosRequestConfig,
): Promise<HeaderResponse<T>> {
  const axiosPromise =
    typeof url === 'string' ? instance(url, config) : instance(url);
  return (axiosPromise as Promise<AxiosResponse<T>>).then(response => [
    response.data,
    response.headers,
  ]);
}

export const getServerDomain = () => {
  return `${window.location.protocol}//${window.location.host}${PUBLIC_URL}`;
};

function unAuthorizationErrorHandler(error: unknown) {
  const responseError = error as ResponseError;
  if (responseError?.response?.status === 401) {
    message.error({ key: '401', content: String(i18next.t('global.401')) });
    removeToken();
    return true;
  }
  throw error;
}

function standardErrorMessageTransformer(error: unknown) {
  const responseError = error as ResponseError;
  if (responseError?.response?.data?.message) {
    console.log('Unhandled Exception | ', responseError.response.data.message);
    return responseError.response.data.message;
  }
  return error;
}
