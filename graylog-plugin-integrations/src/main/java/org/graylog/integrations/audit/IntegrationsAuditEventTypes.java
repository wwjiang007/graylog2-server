/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.integrations.audit;

import com.google.common.collect.ImmutableSet;
import org.graylog2.audit.PluginAuditEventTypes;

import java.util.Set;

public class IntegrationsAuditEventTypes implements PluginAuditEventTypes {
    private static final String NAMESPACE = "integrations:";

    public static final String KINESIS_INPUT_CREATE = NAMESPACE + "kinesis_input:create";

    public static final String KINESIS_SETUP_CREATE_STREAM = NAMESPACE + "kinesis_auto_setup:create_stream";
    public static final String KINESIS_SETUP_CREATE_POLICY = NAMESPACE + "kinesis_auto_setup:create_policy";
    public static final String KINESIS_SETUP_CREATE_SUBSCRIPTION = NAMESPACE + "kinesis_auto_setup:create_subscription";


    private static final Set<String> EVENT_TYPES = ImmutableSet.<String>builder()
            .add(KINESIS_SETUP_CREATE_STREAM)
            .add(KINESIS_SETUP_CREATE_POLICY)
            .add(KINESIS_SETUP_CREATE_SUBSCRIPTION)
            .build();

    @Override
    public Set<String> auditEventTypes() {
        return EVENT_TYPES;
    }
}
