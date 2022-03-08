/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import * as React from 'react';
import { forwardRef, useMemo } from 'react';
import PropTypes from 'prop-types';
import { useTheme } from 'styled-components';

import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';

import StyledAceEditor from './StyledAceEditor';
import type { Editor } from './ace-types';

export type BaseQueryInputProps = {
  className?: string
  error?: QueryValidationState,
  height?: number,
  maxLines?: number,
  placeholder?: string,
  value: string,
  warning?: QueryValidationState,
  wrapEnabled?: boolean,
};

type EnabledInputProps = BaseQueryInputProps & {
  disabled: false,
  enableAutocompletion?: boolean,
  onBlur?: (query: string) => void,
  onChange?: (query: string) => Promise<string>,
  onExecute?: (editor: Editor) => void,
  onLoad?: (editor: Editor) => void,
};
type DisabledInputProps = BaseQueryInputProps & { disabled: true };
type Props = EnabledInputProps | DisabledInputProps

const getMarkers = (errors: QueryValidationState | undefined, warnings: QueryValidationState | undefined) => {
  const markerClassName = 'ace_marker';
  const createMarkers = (explanations = [], className = '') => explanations.map(({
    beginLine,
    beginColumn,
    endLine,
    endColumn,
  }) => ({
    startRow: beginLine,
    startCol: beginColumn,
    endRow: endLine,
    endCol: endColumn,
    type: 'background',
    className,
  }));

  return [
    ...createMarkers(errors?.explanations, `${markerClassName} ace_validation_error`),
    ...createMarkers(warnings?.explanations, `${markerClassName} ace_validation_warning`),
  ];
};

const isEnabledInput = (props: Props): props is EnabledInputProps => !props.disabled;
const isDisabledInput = (props: Props): props is DisabledInputProps => props.disabled;

// Base query input component which is being implemented by the `QueryInput` and `DeactivatedQueryInput`. Should not be used directly, outside this directory.
const BaseQueryInput = forwardRef<StyledAceEditor, Props>((props, ref) => {
  const {
    className,
    disabled,
    error,
    height,
    maxLines,
    placeholder,
    value,
    warning,
    wrapEnabled,
  } = props;
  const theme = useTheme();
  const markers = useMemo(() => getMarkers(error, warning), [error, warning]);

  const commonProps = {
    $height: height,
    aceTheme: 'ace-queryinput', // NOTE: is usually just `theme` but we need that prop for styled-components
    className,
    disabled,
    editorProps: { $blockScrolling: Infinity, selectionStyle: 'line' },
    fontSize: theme.fonts.size.large,
    highlightActiveLine: false,
    markers,
    maxLines,
    minLines: 1,
    mode: 'lucene',
    name: 'QueryEditor',
    placeholder,
    readOnly: disabled,
    ref: ref,
    setOptions: { indentedSoftWrap: false },
    showGutter: false,
    showPrintMargin: false,
    value,
    wrapEnabled,
  };

  if (isDisabledInput(props)) {
    return <StyledAceEditor {...commonProps} disabled />;
  }

  if (isEnabledInput(props)) {
    const {
      onBlur,
      onChange,
      onExecute,
      onLoad,
      enableAutocompletion,
    } = props;

    return (
      <StyledAceEditor {...commonProps}
                       enableBasicAutocompletion={enableAutocompletion}
                       enableLiveAutocompletion={enableAutocompletion}
                       onBlur={onBlur}
                       onChange={onChange}
                       onExecute={onExecute}
                       onLoad={onLoad} />
    );
  }

  return null;
});

BaseQueryInput.propTypes = {
  className: PropTypes.string,
  // @ts-ignore
  disabled: PropTypes.bool,
  enableAutocompletion: PropTypes.bool,
  error: PropTypes.any,
  height: PropTypes.number,
  maxLines: PropTypes.number,
  onBlur: PropTypes.func,
  onChange: PropTypes.func,
  onExecute: PropTypes.func,
  onLoad: PropTypes.func,
  placeholder: PropTypes.string,
  value: PropTypes.string,
  warning: PropTypes.any,
  wrapEnabled: PropTypes.bool,
};

BaseQueryInput.defaultProps = {
  className: '',
  disabled: false,
  enableAutocompletion: false,
  error: undefined,
  height: undefined,
  maxLines: 4,
  onBlur: () => {},
  onChange: () => Promise.resolve(''),
  onExecute: () => {},
  onLoad: () => {},
  placeholder: '',
  value: '',
  warning: undefined,
  wrapEnabled: true,
};

export default BaseQueryInput;
