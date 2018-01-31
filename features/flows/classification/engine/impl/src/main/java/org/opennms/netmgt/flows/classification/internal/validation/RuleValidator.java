/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018-2018 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2018 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.flows.classification.internal.validation;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.opennms.core.utils.IPLike;
import org.opennms.netmgt.flows.classification.error.Errors;
import org.opennms.netmgt.flows.classification.exception.InvalidRuleException;
import org.opennms.netmgt.flows.classification.internal.value.StringValue;
import org.opennms.netmgt.flows.classification.persistence.api.Protocols;
import org.opennms.netmgt.flows.classification.persistence.api.Rule;

import com.google.common.base.Strings;

public class RuleValidator {

    private static final Pattern PROTOCOL_PATTERN = Pattern.compile("^[a-z]+[,a-z]*$", Pattern.CASE_INSENSITIVE);

    private static final Pattern PORT_PATTERN = Pattern.compile("^\\d+((-|,)\\d+)*$");

    public void validate(Rule rule) throws InvalidRuleException {
        // Name is required
        validateName(rule.getName());

        // Ensure that at least one field is defined (no catch all rule)
        if (!rule.hasDefinition()) {
            throw new InvalidRuleException(Errors.RULE_NO_DEFINITIONS);
        }
        // Ensure protocol is defined correctly
        if (rule.hasProtocolDefinition()) {
            validateProtocol(rule.getProtocol());
        }
        // Ensure port is defined correctly
        if (rule.hasPortDefinition()) {
            validatePort(rule.getPort());
        }
        // Ensure ip address is defined correctly
        if (rule.hasIpAddressDefinition()) {
            validateIpAddress(rule.getIpAddress());
        }
    }

    protected static void validateName(String name) {
        if (Strings.isNullOrEmpty(name)) {
            throw new InvalidRuleException(Errors.RULE_NAME_IS_REQUIRED);
        }
    }

    protected static void validateProtocol(String protocol) throws InvalidRuleException {
        if (Strings.isNullOrEmpty(protocol)){
            throw new InvalidRuleException(Errors.RULE_PROTOCOL_IS_REQUIRED);
        }
        final Matcher matcher = PROTOCOL_PATTERN.matcher(protocol);
        if (!matcher.matches()) {
            throw new InvalidRuleException(Errors.RULE_PROTOCOL_DEFINITION_INVALID);
        }
        // Verify protocol actually exists
        for (StringValue eachProtocol : new StringValue(protocol).splitBy(",")) {
            if (Protocols.getProtocol(eachProtocol.getValue()) == null) {
                throw new InvalidRuleException(Errors.RULE_PROTOCOL_DOES_NOT_EXIST, eachProtocol.getValue());
            }
        }
    }

    protected static void validatePort(String port) throws InvalidRuleException {
        final StringValue portValue = new StringValue(port);
        if (portValue.isNullOrEmpty()) {
            throw new InvalidRuleException(Errors.RULE_PORT_IS_REQUIRED);
        }
        if (portValue.hasWildcard()) {
            throw new InvalidRuleException(Errors.RULE_PORT_NO_WILDCARD);
        }

        // Verify input
        final Matcher matcher = PORT_PATTERN.matcher(port);
        if (!matcher.matches()) {
            throw new InvalidRuleException(Errors.RULE_PORT_DEFINITION_NOT_VALID);
        }

        // Try parsing input
        final List<StringValue> portValues = portValue.splitBy(",");
        final List<StringValue> rangedPortValues = portValues.stream().filter(v -> v.isRanged()).collect(Collectors.toList());
        rangedPortValues.forEach(v -> portValues.remove(v));

        // Verify the ranges
        for (StringValue eachRange : rangedPortValues) {
            final List<StringValue> range = eachRange.splitBy("-");

            // Verify each value is a number value
            for (int i=0; i<Math.min(range.size(), 2); i++) {
                verifyPortValue(range.get(i).getValue());
            }

            // Check bounds
            int lowerBound = Integer.parseInt(range.get(0).getValue());
            int higherBound = range.size() == 1 ? lowerBound : Integer.parseInt(range.get(1).getValue());
            if (lowerBound > higherBound) {
                throw new InvalidRuleException(Errors.RULE_PORT_RANGE_BOUNDS_NOT_VALID);
            }
        }

        // Verify normal values
        for (StringValue eachPort : portValues) {
            verifyPortValue(eachPort.getValue());
        }
    }

    protected static void validateIpAddress(String ipAddress) throws InvalidRuleException {
        if (Strings.isNullOrEmpty(ipAddress)) {
            throw new InvalidRuleException(Errors.RULE_IP_ADDRESS_INVALID, ipAddress);
        }
        try {
            StringValue value = new StringValue(ipAddress);
            if (!value.isWildcard()) {
                IPLike.matches("8.8.8.8", ipAddress);
            }
        } catch (Exception ex) {
            throw new InvalidRuleException(Errors.RULE_IP_ADDRESS_INVALID, ipAddress);
        }
    }

    // Validate each port to be between 0 - 65536
    private static void verifyPortValue(String input) throws InvalidRuleException {
        int value = Integer.parseInt(input);
        if (value < 0 || value > 65536) {
            throw new InvalidRuleException(Errors.RULE_PORT_VALUE_NOT_IN_RANGE, 0, 65536);
        }
    }
}
