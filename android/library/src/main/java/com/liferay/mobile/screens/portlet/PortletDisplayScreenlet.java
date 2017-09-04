/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.mobile.screens.portlet;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import com.liferay.mobile.android.auth.Authentication;
import com.liferay.mobile.android.auth.basic.BasicAuthentication;
import com.liferay.mobile.screens.R;
import com.liferay.mobile.screens.base.BaseScreenlet;
import com.liferay.mobile.screens.base.interactor.Interactor;
import com.liferay.mobile.screens.context.LiferayScreensContext;
import com.liferay.mobile.screens.context.LiferayServerContext;
import com.liferay.mobile.screens.context.SessionContext;
import com.liferay.mobile.screens.portlet.util.InjectableScript;
import com.liferay.mobile.screens.portlet.util.JsScript;
import com.liferay.mobile.screens.portlet.view.PortletDisplayViewModel;
import com.liferay.mobile.screens.util.AssetReader;
import com.liferay.mobile.screens.util.LiferayLogger;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;

/**
 * @author Sarai Díaz García
 * @author Victor Galán Grande
 */
public class PortletDisplayScreenlet extends BaseScreenlet<PortletDisplayViewModel, Interactor>
	implements PortletDisplayListener {

	private boolean autoLoad;
	private PortletDisplayListener listener;
	private PortletConfiguration portletConfiguration;
	private boolean isLoggingEnabled = true;
	private boolean isScrollEnabled = true;

	public PortletDisplayScreenlet(Context context) {
		super(context);
	}

	public PortletDisplayScreenlet(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PortletDisplayScreenlet(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public PortletDisplayScreenlet(Context context, AttributeSet attrs, int defStyleAttr,
		int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}

	/**
	 * Starts the operation and check if the Portlet URL it's correct and then loads it.
	 * If the URL is not correct, the operation fails.
	 */
	public void load() {
		getViewModel().showStartOperation(DEFAULT_ACTION);
		if (portletConfiguration != null) {

			String finalUrl = buildPortletUrl(portletConfiguration.getPortletUrl());
			String body = buildBody();

			String screensJs = new AssetReader(getContext()).read(R.raw.screens);

			getViewModel().addScript(new JsScript("Screens.js", screensJs));

			for (InjectableScript script : portletConfiguration.getScripts()) {
				getViewModel().addScript(script);
			}

			if (PortletConfiguration.WebType.LIFERAY_AUTHENTICATED.equals(
				portletConfiguration.getWebType())) {
			WebView webView = getViewModel().getWebView();

			String javascript = "console.log('Ejecutando en Screenlet');"
				+ "var g = function() {console.error('Lanzado en Screenlet');};document.addEventListener('DOMContentLoaded', g, false);";
			String header = "javascript:";

			webView.loadUrl(header + javascript);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				//webView.evaluateJavascript(javascript, null);
			}

				if (SessionContext.isLoggedIn()) {
					getViewModel().postUrl(finalUrl, body);
				} else {
					Exception exception = new Exception(
						"You have to be logged to use the LIFERAY_AUTHENTICATED web type");
					getViewModel().showFailedOperation(DEFAULT_ACTION, exception);
					error(exception, DEFAULT_ACTION);
				}
			} else {
				getViewModel().loadUrl(finalUrl);
			}
		} else {
			getViewModel().showFailedOperation(DEFAULT_ACTION, new MalformedURLException());
		}
	}

	public void injectScript(InjectableScript script) {
		getViewModel().injectScript(script);
	}

	@Override
	public void onPageLoaded(String url) {
		if (listener != null) {
			listener.onPageLoaded(url);
		}
	}

	@Override
	public void onScriptMessageHandler(String namespace, String body) {
		if (namespace.startsWith("screensInternal")) {
			handleInternal(namespace, body);
		} else {
			if (listener != null) {
				listener.onScriptMessageHandler(namespace, body);
			}
		}
	}

	@Override
	public void error(Exception e, String userAction) {
		getViewModel().showFailedOperation(userAction, e);

		if (listener != null) {
			listener.error(e, userAction);
		}
	}

	public boolean isLoggingEnabled() {
		return isLoggingEnabled;
	}

	public void setLoggingEnabled(boolean loggingEnabled) {
		isLoggingEnabled = loggingEnabled;
	}

	public boolean isScrollEnabled() {
		return isScrollEnabled;
	}

	public void setScrollEnabled(boolean enabled) {
		isScrollEnabled = enabled;
		getViewModel().setScrollEnabled(enabled);
	}

	public boolean isAutoLoad() {
		return autoLoad;
	}

	public void setAutoLoad(boolean autoLoad) {
		this.autoLoad = autoLoad;
	}

	public PortletDisplayListener getListener() {
		return listener;
	}

	public void setListener(PortletDisplayListener listener) {
		this.listener = listener;
	}

	public void setPortletConfiguration(PortletConfiguration portletConfiguration) {
		this.portletConfiguration = portletConfiguration;
		getViewModel().configureView(portletConfiguration.isCordovaEnabled(),
			portletConfiguration.getObserver());
	}

	@Override
	protected View createScreenletView(Context context, AttributeSet attributes) {

		TypedArray typedArray = context.getTheme()
			.obtainStyledAttributes(attributes, R.styleable.PortletDisplayScreenlet, 0, 0);

		autoLoad = typedArray.getBoolean(R.styleable.PortletDisplayScreenlet_autoLoad, true);

		int layoutId = typedArray.getResourceId(R.styleable.PortletDisplayScreenlet_layoutId,
			getDefaultLayoutId());

		typedArray.recycle();

		return LayoutInflater.from(context).inflate(layoutId, null);
	}

	@Override
	protected Interactor createInteractor(String actionName) {
		return null;
	}

	@Override
	protected void onUserAction(String userActionName, Interactor interactor, Object... args) {

	}

	protected void autoLoad() {
		if (portletConfiguration != null && portletConfiguration.getPortletUrl() != null) {

			load();
		}
	}

	@Override
	protected void onScreenletAttached() {
		if (autoLoad) {
			autoLoad();
		}
	}

	private void injectScriptInMainThread(final InjectableScript script) {
		LiferayScreensContext.getActivityFromContext(getContext()).runOnUiThread(new Runnable() {
			@Override
			public void run() {
				getViewModel().injectScript(script);
			}
		});
	}

	private String buildPortletUrl(String url) {
		try {
			url = URLEncoder.encode(url, "UTF-8");
			return String.format("%s/c/portal/login?redirect=%s", LiferayServerContext.getServer(),
				url);
		} catch (UnsupportedEncodingException e) {
			return "";
		}
	}

	private String buildBody() {
		Authentication authentication = SessionContext.getAuthentication();

		if (authentication instanceof BasicAuthentication) {
			BasicAuthentication basicAuth = (BasicAuthentication) authentication;

			String username = basicAuth.getUsername();
			String password = basicAuth.getPassword();

			return String.format("login=%s&password=%s", username, password);
		}

		return "";
	}

	private void handleInternal(String namespace, String body) {
		if (namespace.endsWith("error") || namespace.endsWith("consoleMessage")) {
			if (isLoggingEnabled) {
				LiferayLogger.d(body);
			}
		}
	}
}
