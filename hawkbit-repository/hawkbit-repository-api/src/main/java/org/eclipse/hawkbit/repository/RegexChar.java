/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Regular expression characters providing their encoded and their readable
 * expression
 */
public class RegexChar {

    public static RegexChar WHITESPACE = new RegexChar("\\s", "whitespace");
    public static RegexChar DIGITS = new RegexChar("0-9", "digits");
    public static RegexChar QUOTATION_MARKS = new RegexChar("'\"", "quotation marks");
    public static RegexChar SLASHES = new RegexChar("\\/\\\\", "slashes");
    public static RegexChar GREATER_THAN = new RegexChar(">");
    public static RegexChar LESS_THAN = new RegexChar("<");
    public static RegexChar EQUALS = new RegexChar("=");
    public static RegexChar EXCLAMATION_MARK = new RegexChar("!");
    public static RegexChar QUESRION_MARK = new RegexChar("?");
    
    public static Set<RegexChar> getImmutableCharSet(final RegexChar... chars) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(chars)));
    }

    public final String regExp;
    public final String readableExp;

    private RegexChar(final String character) {
        this(character, character);
    }

    private RegexChar(final String regExp, final String readableExp) {
        this.regExp = regExp;
        this.readableExp = readableExp;
    }

    @Override
    public boolean equals(final Object obj){
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RegexChar other = (RegexChar) obj;
        return this.hashCode() == other.hashCode();
    }

    @Override
    public String toString() {
        return readableExp;
    }

    @Override
    public int hashCode() {
        return regExp.hashCode();
    }

}
