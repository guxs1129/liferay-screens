package com.liferay.mobile.screens.viewsets.defaultviews.portlet;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import com.liferay.mobile.screens.R;
import com.liferay.mobile.screens.base.BaseScreenlet;
import com.liferay.mobile.screens.context.LiferayScreensContext;
import com.liferay.mobile.screens.portlet.PortletDisplayScreenlet;
import com.liferay.mobile.screens.portlet.util.InjectableScript;
import com.liferay.mobile.screens.portlet.view.PortletDisplayViewModel;
import com.liferay.mobile.screens.util.LiferayLogger;
import com.liferay.mobile.screens.viewsets.defaultviews.portlet.cordova.CordovaLifeCycleObserver;
import com.liferay.mobile.screens.viewsets.defaultviews.portlet.cordova.ScreensCordovaWebView;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Sarai Díaz García
 * @author Víctor Galán
 */

public class PortletDisplayView extends FrameLayout
	implements PortletDisplayViewModel, ScreensWebView.Listener {

	private BaseScreenlet screenlet;
	private WebView webView;
	private ScreensWebView screensWebView;
	private ProgressBar progressBar;
	private static final String URL_LOGIN = "/c/portal/login";
	private List<InjectableScript> scriptsToInject = new ArrayList<>();
	private boolean isLoaded;

	public PortletDisplayView(Context context) {
		super(context);
	}

	public PortletDisplayView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PortletDisplayView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		progressBar = (ProgressBar) findViewById(R.id.liferay_portlet_progress);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();

		if (screensWebView != null) {
			screensWebView.onAttached();
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();

		if (screensWebView != null) {
			screensWebView.onDetached();
		}
	}

	@Override
	public void showStartOperation(String actionName) {
		if (progressBar != null) {
			progressBar.setVisibility(VISIBLE);
		}
		if (webView != null) {
			webView.setVisibility(GONE);
		}
	}

	@Override
	public void injectScript(InjectableScript script) {
		//injectScript("window.currentFile = '" + script.getName() + "';");
		//injectScript(script.getContent());
	}

	@Override
	public void addScript(InjectableScript script) {
		scriptsToInject.add(script);
	}

	public void injectScript(final String js) {
		String wrapped = "javascript:(function() {" + js + "})()";
		webView.loadUrl(wrapped);
	}

	@Override
	public void postUrl(String url, String body) {
		if (webView != null) {
			webView.postUrl(url, body.getBytes());
		}
	}

	@Override
	public void loadUrl(String url) {
		if (webView != null) {
			webView.loadUrl(url);
		}
	}

	@Override
	public void showFailedOperation(String actionName, Exception e) {
		if (progressBar != null) {
			progressBar.setVisibility(GONE);
		}
		if (webView != null) {
			webView.setVisibility(VISIBLE);
		}

		LiferayLogger.e("Error with portlet", e);
	}

	@Override
	public void showFinishOperation(String actionName) {
		throw new UnsupportedOperationException("showFinishOperation(String) is not supported."
			+ " Use showFinishOperation(String, String) instead.");
	}

	@Override
	public BaseScreenlet getScreenlet() {
		return screenlet;
	}

	@Override
	public void setScreenlet(BaseScreenlet screenlet) {
		this.screenlet = screenlet;
	}

	@Override
	public void configureView(boolean isCordovaEnabled, CordovaLifeCycleObserver observer) {
		if (screensWebView != null) {
			isLoaded = false;
			return;
		}

		if (isCordovaEnabled) {
			screensWebView = new ScreensCordovaWebView(
				LiferayScreensContext.getActivityFromContext(getContext()), observer);
		} else {
			screensWebView = new ScreensNativeWebView(
				LiferayScreensContext.getActivityFromContext(getContext()));
		}

		screensWebView.setListener(this);

		webView = screensWebView.getView();
		webView.setLayoutParams(
			new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.MATCH_PARENT));

		webView.getSettings().setJavaScriptEnabled(true);

		addView(webView);
	}

	@Override
	public void setScrollEnabled(boolean enabled) {
		if (enabled) {
			webView.setOnTouchListener(null);
		} else {
			webView.setOnTouchListener(new View.OnTouchListener() {
				@Override
				public boolean onTouch(View v, MotionEvent event) {
					return (event.getAction() == MotionEvent.ACTION_MOVE);
				}
			});
		}
	}

	@Override
	public void onPageStarted() {
		webView.addJavascriptInterface(new PortletDisplayInterface(), "android");
	}

	@Override
	public void onPageFinished(String url) {
		if (!isLoaded && !url.contains(URL_LOGIN)) {
			isLoaded = true;

			for (InjectableScript script : scriptsToInject) {
				injectScript(script);
			}

			// The webview shows a white screen before loading its content, this prevent that.
			webView.setAlpha(0);
			webView.setVisibility(VISIBLE);
			webView.animate().setStartDelay(500).alpha(1.0f);

			progressBar.setVisibility(GONE);

			((PortletDisplayScreenlet) getScreenlet()).onPageLoaded(url);
		}
	}

	@Override
	public void onPageError(Exception e) {
		((PortletDisplayScreenlet) getScreenlet()).error(e, PortletDisplayScreenlet.DEFAULT_ACTION);
	}

	private class PortletDisplayInterface {

		private PortletDisplayInterface() {
		}

		@JavascriptInterface
		public void postMessage(String namespace, String body) {
			((PortletDisplayScreenlet) getScreenlet()).onScriptMessageHandler(namespace, body);
		}
	}
}
