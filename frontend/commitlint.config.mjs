import standardVersionConfig from './.versionrc.js';

const typeEnums = standardVersionConfig.types.map(t => t.type);

export default {
  extends: ['@commitlint/config-conventional'],
  rules: {
    'type-enum': [2, 'always', typeEnums],
  },
};
