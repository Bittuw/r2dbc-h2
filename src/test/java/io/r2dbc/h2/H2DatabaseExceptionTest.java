/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.r2dbc.h2;

import org.h2.message.DbException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class H2DatabaseExceptionTest {

    @Test
    void initializes() {
        DbException delegate = DbException.get(0);

        H2DatabaseException exception = new H2DatabaseException(delegate);

        assertThat(exception.getMessage()).isEqualTo(delegate.getSQLException().getMessage());
        assertThat(exception.getSqlState()).isEqualTo(delegate.getSQLException().getSQLState());
        assertThat(exception.getErrorCode()).isEqualTo(delegate.getSQLException().getErrorCode());
        assertThat(exception.getCause()).isEqualTo(delegate);
    }
}