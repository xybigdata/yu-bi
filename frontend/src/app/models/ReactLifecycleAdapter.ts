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

import React from 'react';
import type { Root } from 'react-dom/client';
import { createRoot } from 'react-dom/client';

type ReactComponentFactoryDependencies = {
  React: typeof React;
  createRoot: typeof createRoot;
  [key: string]: unknown;
};

type ReactComponentFactory =
  | unknown
  | ((dependencies: ReactComponentFactoryDependencies) => unknown);

interface ReactLifecycleAdapterProps {
  mounted: (
    container: Element | null,
    options?: unknown,
    context?: unknown,
  ) => Root | null;
  updated: (options?: unknown, context?: unknown) => Root | null;
  unmount: () => void;
  resize: (options?: unknown, context?: unknown) => Root | null;
  init: (componentWrapper: ReactComponentFactory) => void;
}

export default class ReactLifecycleAdapter implements ReactLifecycleAdapterProps {
  private domContainer: Element | null = null;
  private reactComponent: ReactComponentFactory;
  private externalLibs: unknown = {};
  private root: Root | null = null;

  constructor(componentWrapper: ReactComponentFactory) {
    this.reactComponent = componentWrapper;
  }

  public init(componentWrapper: ReactComponentFactory) {
    this.reactComponent = componentWrapper;
  }

  public registerImportDependencies(dependencies: unknown) {
    this.externalLibs = dependencies;
  }

  public mounted(
    container: Element | null,
    options?: unknown,
    context?: unknown,
  ) {
    this.domContainer = container;
    return this.render(options, context);
  }

  public updated(options?: unknown, context?: unknown) {
    return this.render(options, context);
  }

  public unmount() {
    this.root?.unmount();
    this.root = null;
    this.domContainer = null;
  }

  public resize(options?: unknown, context?: unknown) {
    return this.render(options, context);
  }

  private render(options?: unknown, context?: unknown) {
    if (!this.domContainer) {
      return null;
    }

    if (!this.root) {
      this.root = createRoot(this.domContainer);
    }

    this.root.render(
      React.createElement(
        this.getComponent() as React.ElementType,
        options,
        context as unknown as React.ReactNode,
      ),
    );
    return this.root;
  }

  private getComponent() {
    if (typeof this.reactComponent === 'function') {
      return (
        this.reactComponent as (
          dependencies: ReactComponentFactoryDependencies,
        ) => unknown
      )({
        React,
        createRoot,
        ...(this.externalLibs as object),
      });
    }
    return this.reactComponent;
  }
}
