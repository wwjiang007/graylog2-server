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
import styled from 'styled-components';

import Button from 'components/bootstrap/Button';
import ButtonToolbar from 'components/bootstrap/ButtonToolbar';
import type { IconName } from 'components/common/Icon';
import Icon from 'components/common/Icon';
import Spinner from 'components/common/Spinner';

const StyledButtonToolbar = styled(ButtonToolbar)`
  display: flex;
  justify-content: flex-end;
  align-items: end;
`;

type WithCancelProps = {
  displayCancel: true,
  disableCancel?: boolean,
  onCancel: () => void,
}

type WithoutCancelProps = {
  displayCancel: false
}

type Props = {
  bsSize?: 'large' | 'small' | 'xsmall',
  className?: string,
  displayCancel?: boolean,
  disabledSubmit?: boolean,
  formId?: string,
  isSubmitting?: boolean,
  leftCol?: React.ReactNode,
  onSubmit?: () => void,
  submitButtonText: string,
  submitIcon?: IconName,
  submitButtonType?: 'submit' | 'button',
  submitLoadingText?: string,
} & (WithCancelProps | WithoutCancelProps)

const ModalSubmit = (props: Props) => {
  const {
    bsSize,
    className,
    displayCancel,
    disabledSubmit,
    formId,
    isSubmitting,
    leftCol,
    onSubmit,
    submitButtonText,
    submitButtonType,
    submitIcon,
    submitLoadingText,
  } = props;

  return (
    <StyledButtonToolbar className={className}>
      {leftCol}
      {displayCancel && (
        <Button type="button"
                bsSize={bsSize}
                onClick={props.onCancel}
                disabled={props.disableCancel || isSubmitting}>
          Cancel
        </Button>
      )}
      <Button bsStyle="success"
              bsSize={bsSize}
              disabled={disabledSubmit}
              form={formId}
              title={submitButtonText}
              type={submitButtonType}
              onClick={onSubmit}>
        {(submitIcon && !isSubmitting) && <><Icon name={submitIcon} /> </>}
        {isSubmitting ? <Spinner text={submitLoadingText} delay={0} /> : submitButtonText}
      </Button>
    </StyledButtonToolbar>
  );
};

ModalSubmit.defaultProps = {
  bsSize: undefined,
  className: undefined,
  disabledSubmit: false,
  displayCancel: true,
  formId: undefined,
  isSubmitting: false,
  leftCol: undefined,
  onSubmit: undefined,
  submitButtonType: 'submit',
  submitIcon: undefined,
  submitLoadingText: undefined,
};

export default ModalSubmit;