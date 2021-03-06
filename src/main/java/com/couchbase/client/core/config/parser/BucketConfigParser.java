/*
 * Copyright (c) 2016 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.couchbase.client.core.config.parser;

import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.config.BucketConfig;
import com.couchbase.client.core.env.ConfigParserEnvironment;
import com.couchbase.client.core.utils.DefaultObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.InjectableValues;

import java.io.IOException;

/**
 * An abstraction over the bucket parser which takes a raw config as a string and turns it into a
 * {@link BucketConfig}.
 *
 * @author Michael Nitschinger
 * @since 2.0.0
 */
public final class BucketConfigParser {
    /**
     * Parse a raw configuration into a {@link BucketConfig}.
     *
     * @param input the raw string input.
     * @return the parsed bucket configuration.
     */
    public static BucketConfig parse(final String input, final ConfigParserEnvironment env) {
        try {
            InjectableValues inject = new InjectableValues.Std()
                    .addValue("env", env);
            return DefaultObjectMapper.reader()
                    .forType(BucketConfig.class)
                    .with(inject)
                    .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
                    .readValue(input);
        } catch (IOException e) {
            throw new CouchbaseException("Could not parse configuration", e);
        }
    }
}
