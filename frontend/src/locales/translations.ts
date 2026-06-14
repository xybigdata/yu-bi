import { ConvertedToObjectType, TranslationJsonType } from './types';

/**
 * This file is seperate from the './i18n.ts' simply to make the Hot Module Replacement work seamlessly.
 * Your components can import this file in 'messages.ts' files which would ruin the HMR if this isn't a separate module
 */
export const translations: ConvertedToObjectType<TranslationJsonType> =
  {} as ConvertedToObjectType<TranslationJsonType>;

type TranslationBranch = {
  [key: string]: string | TranslationBranch;
};

/*
 * Converts the static JSON file into an object where keys are identical
 * but values are strings concatenated according to syntax.
 * This is helpful when using the JSON file keys and still having the intellisense support
 * along with type-safety
 */
export const convertLanguageJsonToObject = (
  json: TranslationBranch,
  objToConvertTo: TranslationBranch = translations,
  current?: string,
) => {
  Object.keys(json).forEach(key => {
    const currentLookupKey = current ? `${current}.${key}` : key;
    const currentValue = json[key];

    if (currentValue && typeof currentValue === 'object') {
      objToConvertTo[key] = {};
      convertLanguageJsonToObject(
        currentValue,
        objToConvertTo[key] as TranslationBranch,
        currentLookupKey,
      );
    } else {
      objToConvertTo[key] = currentLookupKey;
    }
  });
};
