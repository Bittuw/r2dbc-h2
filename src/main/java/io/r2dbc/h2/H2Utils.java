/*
 * Copyright 2018 the original author or authors.
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

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.h2.command.CommandInterface;
import org.h2.engine.SessionInterface;
import org.h2.result.ResultInterface;
import org.h2.result.ResultWithGeneratedKeys;
import org.h2.value.Value;
import org.h2.value.ValueDouble;
import org.h2.value.ValueInt;
import org.h2.value.ValueNull;
import org.h2.value.ValueString;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Greg Turnquist
 */
@Slf4j
final class H2Utils {

	/**
	 * Wrap H2 SQL query in Reactor flow.
	 *
	 * @param sql
	 * @param bindings
	 * @param maxRows
	 * @return
	 */
	static Flux<ResultInterface> query(SessionInterface session, String sql, List<Binding> bindings, int maxRows) {

		Objects.requireNonNull(sql, "sql must not be null");

		return Flux.fromIterable(bindings)
			.filter(binding -> !binding.getParameters().isEmpty())
			.map(binding -> queryWithBinding(session, sql, binding, maxRows))
			.switchIfEmpty(Flux.defer(() -> Mono.just(queryWithoutBinding(session, sql, maxRows))));
	}

	static Flux<ResultInterface> query(SessionInterface session, String sql, int maxRows) {
		return query(session, sql, Collections.emptyList(), maxRows);
	}

	/**
	 * Wrap H2 SQL operation in Reactor flow.
	 *
	 * @param sql
	 * @return
	 */
	static Flux<ResultInterface> update(SessionInterface session, String sql, List<Binding> bindings) {

		Objects.requireNonNull(sql, "sql must not be null");

		return Flux.fromIterable(bindings)
			.filter(binding -> !binding.getParameters().isEmpty())
			.map(binding -> updateWithBinding(session, sql, binding))
			.switchIfEmpty(Flux.defer(() -> Mono.just(updateWithoutBinding(session, sql))));
	}

	static Flux<ResultInterface> update(SessionInterface session, String sql) {
		return update(session, sql, Collections.emptyList());
	}

	//------------------------------------------------------------------------------------------

	private static ResultInterface queryWithBinding(SessionInterface session, String sql, Binding binding, int maxRows) {

		if (log.isTraceEnabled()) {
			log.trace(String.format("Executing query '%s' with '%s' bindings.", sql, binding.getParameters()));
		}

		CommandInterface command = session.prepareCommand(sql, Integer.MAX_VALUE);

		bindParametersToCommand(binding, command);

		return command.executeQuery(maxRows, false);
	}

	private static ResultInterface queryWithoutBinding(SessionInterface session, String sql, int maxRows) {

		if (log.isTraceEnabled()) {
			log.trace(String.format("Executing query '%s' with NO bindings.", sql));
		}

		CommandInterface command = session.prepareCommand(sql, Integer.MAX_VALUE);

		return command.executeQuery(maxRows, false);
	}

	private static ResultInterface updateWithBinding(SessionInterface session, String sql, Binding binding) {

		if (log.isTraceEnabled()) {
			log.trace(String.format("Executing update '%s' with '%s' bindings.", sql, binding.getParameters()));
		}

		CommandInterface command = session.prepareCommand(sql, Integer.MAX_VALUE);

		bindParametersToCommand(binding, command);

		ResultWithGeneratedKeys resultWithGeneratedKeys = command.executeUpdate(true);
		return resultWithGeneratedKeys.getGeneratedKeys();
	}

	private static ResultInterface updateWithoutBinding(SessionInterface session, String sql) {

		if (log.isTraceEnabled()) {
			log.trace(String.format("Executing update '%s' with NO bindings.", sql));
		}

		CommandInterface command = session.prepareCommand(sql, Integer.MAX_VALUE);

		ResultWithGeneratedKeys resultWithGeneratedKeys = command.executeUpdate(true);
		return resultWithGeneratedKeys.getGeneratedKeys();
	}

	private static void bindParametersToCommand(Binding binding, CommandInterface command) {

		binding.getParameters().forEach(entry -> {
			command.getParameters().get(entry.getKey()).setValue(toValue(entry.getValue()), false);
		});

	}

	private static Value toValue(Object object) {

		if (object == String.class) {
			return ValueString.get((String) object);
		} else if (object instanceof Integer) {
			return ValueInt.get((Integer) object);
		} else if (object == Double.class) {
			return ValueDouble.get((Double) object);
		} else if (object == ValueNull.INSTANCE) {
			return ValueNull.INSTANCE;
		}

		throw new UnsupportedOperationException(String.format("Can't handle '%s' types", object.getClass()));
	}
}