/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.sma.internal;

import static org.openhab.binding.sma.internal.SmaBindingConstants.*;

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.sma.internal.handler.SmaBridgeHandler;
import org.openhab.binding.sma.internal.handler.SmaHandler;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.i18n.TranslationProvider;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link SmaHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Martin Gerczuk - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "binding.sma", service = ThingHandlerFactory.class)
public class SmaHandlerFactory extends BaseThingHandlerFactory {

    private ComponentContext componentContext;

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_BRIDGE, THING_TYPE_INVERTER);

    public static final Set<ThingTypeUID> DISCOVERABLE_THING_TYPES_UIDS = Collections.singleton(THING_TYPE_INVERTER);

    @Activate
    public SmaHandlerFactory(final @Reference LocaleProvider localeProvider,
            final @Reference TranslationProvider i18nProvider, ComponentContext componentContext) {
        super.activate(componentContext);
        this.componentContext = componentContext;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_BRIDGE)) {
            return new SmaBridgeHandler((Bridge) thing, componentContext.getBundleContext());
        }
        if (thingTypeUID.equals(THING_TYPE_INVERTER)) {
            return new SmaHandler(thing);
        }

        return null;
    }
}
