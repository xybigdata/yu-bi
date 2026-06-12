type QueryPrimitive = string | number | boolean | null | undefined;
type QueryValue =
  | QueryPrimitive
  | QueryValue[]
  | {
      [key: string]: QueryValue;
    };

type QueryObject = Record<string, QueryValue>;

type StringifyOptions = {
  arrayFormat?: 'indices' | 'brackets';
};

type ParseOptions = {
  ignoreQueryPrefix?: boolean;
};

const defaultStringifyOptions: Required<StringifyOptions> = {
  arrayFormat: 'indices',
};

function isPlainObject(value: unknown): value is QueryObject {
  return Object.prototype.toString.call(value) === '[object Object]';
}

function isArrayToken(token: string) {
  return token === '' || /^\d+$/.test(token);
}

function createContainer(nextToken?: string) {
  return isArrayToken(nextToken || '') ? [] : {};
}

function appendQueryEntry(
  params: URLSearchParams,
  key: string,
  value: QueryValue,
  options: Required<StringifyOptions>,
) {
  if (value === undefined) {
    return;
  }

  if (value === null) {
    params.append(key, '');
    return;
  }

  if (Array.isArray(value)) {
    value.forEach((item, index) => {
      const itemKey =
        options.arrayFormat === 'brackets' &&
        !Array.isArray(item) &&
        !isPlainObject(item)
          ? `${key}[]`
          : `${key}[${index}]`;
      appendQueryEntry(params, itemKey, item, options);
    });
    return;
  }

  if (isPlainObject(value)) {
    Object.entries(value).forEach(([childKey, childValue]) => {
      appendQueryEntry(params, `${key}[${childKey}]`, childValue, options);
    });
    return;
  }

  params.append(key, String(value));
}

function tokenizeQueryKey(key: string) {
  const tokens: string[] = [];
  const pattern = /([^[\]]+)|\[(.*?)\]/g;

  key.replace(pattern, (_, directToken, nestedToken) => {
    tokens.push(directToken ?? nestedToken ?? '');
    return '';
  });

  return tokens;
}

function setParsedValue(target: unknown, tokens: string[], value: string): unknown {
  if (!tokens.length) {
    return value;
  }

  const [token, ...restTokens] = tokens;

  if (token === '') {
    const arrayTarget = Array.isArray(target) ? target : [];
    if (!restTokens.length) {
      arrayTarget.push(value);
      return arrayTarget;
    }

    arrayTarget.push(
      setParsedValue(createContainer(restTokens[0]), restTokens, value),
    );
    return arrayTarget;
  }

  if (/^\d+$/.test(token)) {
    const arrayTarget = Array.isArray(target) ? target : [];
    const index = Number(token);
    if (!restTokens.length) {
      arrayTarget[index] = value;
      return arrayTarget;
    }

    arrayTarget[index] = setParsedValue(
      arrayTarget[index] ?? createContainer(restTokens[0]),
      restTokens,
      value,
    );
    return arrayTarget;
  }

  const objectTarget = isPlainObject(target) ? target : {};

  if (!restTokens.length) {
    const existingValue = objectTarget[token];
    if (existingValue === undefined) {
      objectTarget[token] = value;
    } else if (Array.isArray(existingValue)) {
      existingValue.push(value);
    } else {
      objectTarget[token] = [existingValue, value];
    }
    return objectTarget;
  }

  objectTarget[token] = setParsedValue(
    objectTarget[token] ?? createContainer(restTokens[0]),
    restTokens,
    value,
  ) as QueryValue;
  return objectTarget;
}

export function stringifyQuery(
  value: QueryObject,
  options?: StringifyOptions,
): string {
  const params = new URLSearchParams();
  const mergedOptions = {
    ...defaultStringifyOptions,
    ...options,
  };

  Object.entries(value).forEach(([key, queryValue]) => {
    appendQueryEntry(params, key, queryValue, mergedOptions);
  });

  return params.toString();
}

export function parseQuery(
  query: string,
  options?: ParseOptions,
): Record<string, unknown> {
  const source =
    options?.ignoreQueryPrefix && query.startsWith('?')
      ? query.slice(1)
      : query;
  const params = new URLSearchParams(source);
  let result: Record<string, unknown> = {};

  params.forEach((value, key) => {
    result = setParsedValue(result, tokenizeQueryKey(key), value) as Record<
      string,
      unknown
    >;
  });

  return result;
}
