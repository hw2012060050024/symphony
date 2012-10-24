/*
 * Copyright (c) 2012, B3log Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.b3log.symphony.processor;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.b3log.latke.Keys;
import org.b3log.latke.Latkes;
import org.b3log.latke.model.User;
import org.b3log.latke.service.LangPropsService;
import org.b3log.latke.service.ServiceException;
import org.b3log.latke.servlet.HTTPRequestContext;
import org.b3log.latke.servlet.HTTPRequestMethod;
import org.b3log.latke.servlet.annotation.RequestProcessing;
import org.b3log.latke.servlet.annotation.RequestProcessor;
import org.b3log.latke.servlet.renderer.JSONRenderer;
import org.b3log.latke.servlet.renderer.freemarker.AbstractFreeMarkerRenderer;
import org.b3log.latke.servlet.renderer.freemarker.FreeMarkerRenderer;
import org.b3log.latke.util.MD5;
import org.b3log.latke.util.Requests;
import org.b3log.symphony.model.Common;
import org.b3log.symphony.model.UserExt;
import org.b3log.symphony.service.ArticleQueryService;
import org.b3log.symphony.service.CommentQueryService;
import org.b3log.symphony.service.TagQueryService;
import org.b3log.symphony.service.UserMgmtService;
import org.b3log.symphony.service.UserQueryService;
import org.b3log.symphony.util.Filler;
import org.b3log.symphony.util.QueryResults;
import org.b3log.symphony.util.Symphonys;
import org.json.JSONObject;

/**
 * User processor.
 *
 * <p> 
 * For user 
 *   <ul> 
 *     <li>User Home (/{userName}), GET</li> 
 *     <li>Settings (/settings), GET</li> 
 *     <li>Profiles (/settings/profiles), POST</li>
 *     <li>Sync (/settings/b3), POST</li>
 *     <li>Password (/settings/password), POST</li>
 *   </ul> 
 * </p>
 *
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @version 1.0.0.7, Oct 24, 2012
 * @since 0.2.0
 */
@RequestProcessor
public final class UserProcessor {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(UserProcessor.class.getName());
    /**
     * User management service.
     */
    private UserMgmtService userMgmtService = UserMgmtService.getInstance();
    /**
     * Article management service.
     */
    private ArticleQueryService articleQueryService = ArticleQueryService.getInstance();
    /**
     * User query service.
     */
    private UserQueryService userQueryService = UserQueryService.getInstance();
    /**
     * Tag query service.
     */
    private TagQueryService tagQueryService = TagQueryService.getInstance();
    /**
     * Comment query service.
     */
    private CommentQueryService commentQueryService = CommentQueryService.getInstance();
    /**
     * Language service.
     */
    private LangPropsService langPropsService = LangPropsService.getInstance();

    /**
     * Shows user home page.
     *
     * @param context the specified context
     * @param request the specified request
     * @param response the specified response
     * @param userName the specified user name
     * @throws Exception exception
     */
    @RequestProcessing(value = "/{userName}", method = HTTPRequestMethod.GET)
    public void showHome(final HTTPRequestContext context, final HttpServletRequest request, final HttpServletResponse response,
                         final String userName) throws Exception {
        final AbstractFreeMarkerRenderer renderer = new FreeMarkerRenderer();
        context.setRenderer(renderer);
        renderer.setTemplateName("/home/home.ftl");
        final Map<String, Object> dataModel = renderer.getDataModel();

        final JSONObject user = userQueryService.getUserByName(userName);
        if (null == user) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);

            return;
        }

        dataModel.put(User.USER, user);
        fillUserThumbnailURL(user);

        user.put(UserExt.USER_T_CREATE_TIME, new Date(user.getLong(Keys.OBJECT_ID)));

        final List<JSONObject> userArticles = articleQueryService.getUserArticles(
                user.optString(Keys.OBJECT_ID), 1, Symphonys.getInt("userHomeArticlesCnt"));
        dataModel.put(Common.USER_HOME_ARTICLES, userArticles);

        Filler.fillHeader(request, response, dataModel);
        Filler.fillFooter(dataModel);
    }

    /**
     * Shows user home comments page.
     *
     * @param context the specified context
     * @param request the specified request
     * @param response the specified response
     * @param userName the specified user name
     * @throws Exception exception
     */
    @RequestProcessing(value = "/{userName}/comments", method = HTTPRequestMethod.GET)
    public void showHomeComments(final HTTPRequestContext context, final HttpServletRequest request, final HttpServletResponse response,
                                 final String userName) throws Exception {
        final AbstractFreeMarkerRenderer renderer = new FreeMarkerRenderer();
        context.setRenderer(renderer);
        renderer.setTemplateName("/home/comments.ftl");
        final Map<String, Object> dataModel = renderer.getDataModel();

        final JSONObject user = userQueryService.getUserByName(userName);
        if (null == user) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);

            return;
        }

        dataModel.put(User.USER, user);
        fillUserThumbnailURL(user);

        user.put(UserExt.USER_T_CREATE_TIME, new Date(user.getLong(Keys.OBJECT_ID)));

        final List<JSONObject> userComments =
                commentQueryService.getUserComments(user.optString(Keys.OBJECT_ID), 1, Symphonys.getInt("userHomeCmtsCnt"));
        dataModel.put(Common.USER_HOME_COMMENTS, userComments);

        Filler.fillHeader(request, response, dataModel);
        Filler.fillFooter(dataModel);
    }

    /**
     * Shows settings page.
     *
     * @param context the specified context
     * @param request the specified request
     * @param response the specified response
     * @throws Exception exception
     */
    @RequestProcessing(value = "/settings", method = HTTPRequestMethod.GET)
    public void showSettings(final HTTPRequestContext context, final HttpServletRequest request, final HttpServletResponse response)
            throws Exception {
        final AbstractFreeMarkerRenderer renderer = new FreeMarkerRenderer();
        context.setRenderer(renderer);
        renderer.setTemplateName("/home/settings.ftl");
        final Map<String, Object> dataModel = renderer.getDataModel();

        final JSONObject user = LoginProcessor.getCurrentUser(request);

        dataModel.put(User.USER, user);

        fillUserThumbnailURL(user);

        Filler.fillHeader(request, response, dataModel);
        Filler.fillFooter(dataModel);
    }

    /**
     * Updates user profiles.
     *
     * @param context the specified context
     * @param request the specified request
     * @param response the specified response
     * @throws Exception exception
     */
    @RequestProcessing(value = "/settings/profiles", method = HTTPRequestMethod.POST)
    public void updateProfiles(final HTTPRequestContext context, final HttpServletRequest request, final HttpServletResponse response)
            throws Exception {
        final JSONRenderer renderer = new JSONRenderer();
        context.setRenderer(renderer);

        final JSONObject ret = QueryResults.falseResult();
        renderer.setJSONObject(ret);

        final JSONObject requestJSONObject = Requests.parseRequestJSONObject(request, response);

        final String userName = requestJSONObject.optString(User.USER_NAME);
        final String userURL = requestJSONObject.optString(User.USER_URL);
        final String userQQ = requestJSONObject.optString(UserExt.USER_QQ);
        final String userIntro = requestJSONObject.optString(UserExt.USER_INTRO);

        final JSONObject user = LoginProcessor.getCurrentUser(request);
        user.put(User.USER_NAME, userName);
        user.put(User.USER_URL, userURL);
        user.put(UserExt.USER_QQ, userQQ);
        user.put(UserExt.USER_INTRO, userIntro);

        try {
            userMgmtService.updateProfiles(user);
            ret.put(Keys.STATUS_CODE, true);
        } catch (final ServiceException e) {
            final String msg = langPropsService.get("updateFailLabel") + " - " + e.getMessage();
            LOGGER.log(Level.SEVERE, msg, e);

            ret.put(Keys.MSG, msg);
        }
    }

    /**
     * Updates user B3log sync.
     *
     * @param context the specified context
     * @param request the specified request
     * @param response the specified response
     * @throws Exception exception
     */
    @RequestProcessing(value = "/settings/sync/b3", method = HTTPRequestMethod.POST)
    public void updateSyncB3(final HTTPRequestContext context, final HttpServletRequest request, final HttpServletResponse response)
            throws Exception {
        final JSONRenderer renderer = new JSONRenderer();
        context.setRenderer(renderer);

        final JSONObject ret = QueryResults.falseResult();
        renderer.setJSONObject(ret);

        final JSONObject requestJSONObject = Requests.parseRequestJSONObject(request, response);

        final String b3Key = requestJSONObject.optString(UserExt.USER_B3_KEY);
        final String addArticleURL = requestJSONObject.optString(UserExt.USER_B3_CLIENT_ADD_ARTICLE_URL);
        final String addCommentURL = requestJSONObject.optString(UserExt.USER_B3_CLIENT_ADD_COMMENT_URL);

        final JSONObject user = LoginProcessor.getCurrentUser(request);
        user.put(UserExt.USER_B3_KEY, b3Key);
        user.put(UserExt.USER_B3_CLIENT_ADD_ARTICLE_URL, addArticleURL);
        user.put(UserExt.USER_B3_CLIENT_ADD_COMMENT_URL, addCommentURL);

        try {
            userMgmtService.updateSyncB3(user);
            ret.put(Keys.STATUS_CODE, true);
        } catch (final ServiceException e) {
            final String msg = langPropsService.get("updateFailLabel") + " - " + e.getMessage();
            LOGGER.log(Level.SEVERE, msg, e);

            ret.put(Keys.MSG, msg);
        }
    }

    /**
     * Updates user password.
     *
     * @param context the specified context
     * @param request the specified request
     * @param response the specified response
     * @throws Exception exception
     */
    @RequestProcessing(value = "/settings/password", method = HTTPRequestMethod.POST)
    public void updatePassword(final HTTPRequestContext context, final HttpServletRequest request, final HttpServletResponse response)
            throws Exception {
        final JSONRenderer renderer = new JSONRenderer();
        context.setRenderer(renderer);

        final JSONObject ret = QueryResults.falseResult();
        renderer.setJSONObject(ret);

        final JSONObject requestJSONObject = Requests.parseRequestJSONObject(request, response);

        final String password = requestJSONObject.optString(User.USER_PASSWORD);
        final String newPassword = requestJSONObject.optString(User.USER_NEW_PASSWORD);

        final JSONObject user = LoginProcessor.getCurrentUser(request);

        if (!password.equals(user.optString(User.USER_PASSWORD))) {
            ret.put(Keys.MSG, "old pwd error");

            return;
        }

        user.put(User.USER_PASSWORD, newPassword);

        try {
            userMgmtService.updatePassword(user);
            ret.put(Keys.STATUS_CODE, true);
        } catch (final ServiceException e) {
            final String msg = langPropsService.get("updateFailLabel") + " - " + e.getMessage();
            LOGGER.log(Level.SEVERE, msg, e);

            ret.put(Keys.MSG, msg);
        }
    }

    /**
     * Fills the specified user thumbnail URL.
     * 
     * @param user the specified user
     */
    private void fillUserThumbnailURL(final JSONObject user) {
        final String userEmail = user.optString(User.USER_EMAIL);
        final String thumbnailURL = "http://secure.gravatar.com/avatar/" + MD5.hash(userEmail) + "?s=140&d="
                                    + Latkes.getStaticServePath() + "/images/user-thumbnail.png";
        user.put(UserExt.USER_T_THUMBNAIL_URL, thumbnailURL);
    }
}
