import { parseQuery, stringifyQuery } from 'utils/queryString';

describe('queryString', () => {
  it('should stringify bracket arrays for request params', () => {
    const query = stringifyQuery(
      {
        sourceId: '1',
        snippet: 'a',
        ids: ['x', 'y'],
      },
      { arrayFormat: 'brackets' },
    );

    expect(query).toBe('sourceId=1&snippet=a&ids%5B%5D=x&ids%5B%5D=y');
  });

  it('should stringify nested filters and variables for jump urls', () => {
    const query = stringifyQuery({
      filters: [
        {
          column: 'city',
          sqlOperator: 'IN',
          values: [
            {
              value: 'hz',
              valueType: 'STRING',
            },
          ],
        },
      ],
      variables: {
        region: ['east', 'west'],
      },
    });

    expect(query).toBe(
      'filters%5B0%5D%5Bcolumn%5D=city&filters%5B0%5D%5BsqlOperator%5D=IN&filters%5B0%5D%5Bvalues%5D%5B0%5D%5Bvalue%5D=hz&filters%5B0%5D%5Bvalues%5D%5B0%5D%5BvalueType%5D=STRING&variables%5Bregion%5D%5B0%5D=east&variables%5Bregion%5D%5B1%5D=west',
    );
  });

  it('should parse nested filters and variables from jump urls', () => {
    const parsed = parseQuery(
      '?filters%5B0%5D%5Bcolumn%5D=city&filters%5B0%5D%5BsqlOperator%5D=IN&filters%5B0%5D%5Bvalues%5D%5B0%5D%5Bvalue%5D=hz&filters%5B0%5D%5Bvalues%5D%5B0%5D%5BvalueType%5D=STRING&variables%5Bregion%5D%5B0%5D=east&variables%5Bregion%5D%5B1%5D=west',
      {
        ignoreQueryPrefix: true,
      },
    );

    expect(parsed).toEqual({
      filters: [
        {
          column: 'city',
          sqlOperator: 'IN',
          values: [
            {
              value: 'hz',
              valueType: 'STRING',
            },
          ],
        },
      ],
      variables: {
        region: ['east', 'west'],
      },
    });
  });
});
