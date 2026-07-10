import { describe, expect, test } from 'vitest';
import {
  getDatasourceFileUploadError,
  getDatasourceFileUploadAction,
  isDatasourceFileUploading,
  isDatasourceFileUploadSettled,
} from '../FileUpload';

describe('FileUpload helpers', () => {
  test('only treats uploading status as active upload', () => {
    expect(isDatasourceFileUploading('uploading')).toBe(true);
    expect(isDatasourceFileUploading('done')).toBe(false);
    expect(isDatasourceFileUploading('error')).toBe(false);
    expect(isDatasourceFileUploading('removed')).toBe(false);
    expect(isDatasourceFileUploading(undefined)).toBe(false);
  });

  test('treats terminal upload statuses as settled', () => {
    expect(isDatasourceFileUploadSettled('done')).toBe(true);
    expect(isDatasourceFileUploadSettled('error')).toBe(true);
    expect(isDatasourceFileUploadSettled('removed')).toBe(true);
    expect(isDatasourceFileUploadSettled('uploading')).toBe(false);
    expect(isDatasourceFileUploadSettled(undefined)).toBe(false);
  });

  test('prefers response message over exception for upload errors', () => {
    expect(
      getDatasourceFileUploadError({
        message: '业务失败',
        exception: 'stack trace',
      }),
    ).toBe('业务失败');
    expect(getDatasourceFileUploadError({ exception: 'stack trace' })).toBe(
      'stack trace',
    );
    expect(getDatasourceFileUploadError()).toBeUndefined();
  });

  test('builds datasource upload action without trailing slash', () => {
    expect(getDatasourceFileUploadAction('source-1')).toBe(
      '/api/v1/files/datasource?sourceId=source-1',
    );
  });
});
