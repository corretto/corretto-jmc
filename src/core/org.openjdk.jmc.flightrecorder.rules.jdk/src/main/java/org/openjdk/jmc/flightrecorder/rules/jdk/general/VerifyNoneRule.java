/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The contents of this file are subject to the terms of either the Universal Permissive License
 * v 1.0 as shown at http://oss.oracle.com/licenses/upl
 *
 * or the following license:
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openjdk.jmc.flightrecorder.rules.jdk.general;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;

import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.util.IPreferenceValueProvider;
import org.openjdk.jmc.common.util.TypedPreference;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.openjdk.jmc.flightrecorder.rules.IResult;
import org.openjdk.jmc.flightrecorder.rules.IResultValueProvider;
import org.openjdk.jmc.flightrecorder.rules.IRule;
import org.openjdk.jmc.flightrecorder.rules.ResultBuilder;
import org.openjdk.jmc.flightrecorder.rules.Severity;
import org.openjdk.jmc.flightrecorder.rules.TypedResult;
import org.openjdk.jmc.flightrecorder.rules.jdk.messages.internal.Messages;
import org.openjdk.jmc.flightrecorder.rules.util.JfrRuleTopics;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.EventAvailability;
import org.openjdk.jmc.flightrecorder.rules.util.RulesToolkit.RequiredEventsBuilder;

public class VerifyNoneRule implements IRule {
	private static final String VERIFYNONE_RESULT_ID = "BytecodeVerification"; //$NON-NLS-1$
	// FIXME: JMC-4617 - Merge with OptionsCheckRule?

	private static final Map<String, EventAvailability> REQUIRED_EVENTS = RequiredEventsBuilder.create()
			.addEventType(JdkTypeIDs.VM_INFO, EventAvailability.AVAILABLE).build();

	private IResult getResult(
		IItemCollection items, IPreferenceValueProvider valueProvider, IResultValueProvider resultProvider) {
		String verifyNone = RulesToolkit.findMatches(JdkTypeIDs.VM_INFO, items, JdkAttributes.JVM_ARGUMENTS,
				"\\-Xverify:none", false); //$NON-NLS-1$
		if (verifyNone == null) {
			verifyNone = RulesToolkit.findMatches(JdkTypeIDs.VM_INFO, items, JdkAttributes.JAVA_ARGUMENTS,
					"\\-Xverify:none", false); //$NON-NLS-1$
		}
		// FIXME: Possibly not needed, seems to be translated to -Xverify:none.
		String noVerify = RulesToolkit.findMatches(JdkTypeIDs.VM_INFO, items, JdkAttributes.JVM_ARGUMENTS,
				"\\-noverify*", false); //$NON-NLS-1$
		if (noVerify == null) {
			noVerify = RulesToolkit.findMatches(JdkTypeIDs.VM_INFO, items, JdkAttributes.JAVA_ARGUMENTS, "\\-noverify*", //$NON-NLS-1$
					false);
		}
		if (verifyNone != null || noVerify != null) {
			// FIXME: Improve check, possibly make it configurable for the user?
			String wls = RulesToolkit.findMatches(JdkTypeIDs.VM_INFO, items, JdkAttributes.JAVA_ARGUMENTS,
					"weblogic.Server", false); //$NON-NLS-1$
			if (wls != null) {
				return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.OK)
						.setSummary(Messages.getString(Messages.VerifyNoneRule_WLS_TEXT_INFO))
						.setExplanation(Messages.getString(Messages.VerifyNoneRule_WLS_TEXT_INFO_LONG)).build();
			}
			String argument = verifyNone != null ? verifyNone : noVerify;
			return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.WARNING)
					.setSummary(Messages.getString(Messages.VerifyNoneRule_TEXT_INFO))
					.setExplanation(
							MessageFormat.format(Messages.getString(Messages.VerifyNoneRule_TEXT_INFO_LONG), argument))
					.build();
		}
		return ResultBuilder.createFor(this, valueProvider).setSeverity(Severity.OK)
				.setSummary(Messages.getString(Messages.VerifyNoneRule_TEXT_OK)).build();
	}

	@Override
	public RunnableFuture<IResult> createEvaluation(
		final IItemCollection items, final IPreferenceValueProvider valueProvider,
		final IResultValueProvider resultProvider) {
		FutureTask<IResult> evaluationTask = new FutureTask<>(new Callable<IResult>() {
			@Override
			public IResult call() throws Exception {
				return getResult(items, valueProvider, resultProvider);
			}
		});
		return evaluationTask;
	}

	@Override
	public Collection<TypedPreference<?>> getConfigurationAttributes() {
		return Collections.emptyList();
	}

	@Override
	public String getId() {
		return VERIFYNONE_RESULT_ID;
	}

	@Override
	public String getName() {
		return Messages.getString(Messages.VerifyNoneRule_RULE_NAME);
	}

	@Override
	public String getTopic() {
		return JfrRuleTopics.JVM_INFORMATION;
	}

	@Override
	public Map<String, EventAvailability> getRequiredEvents() {
		return REQUIRED_EVENTS;
	}

	@Override
	public Collection<TypedResult<?>> getResults() {
		return Collections.emptyList();
	}
}
