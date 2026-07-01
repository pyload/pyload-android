package org.pyload.android.openapi.api;

import org.pyload.android.openapi.CollectionFormats.*;

import retrofit2.Call;
import retrofit2.http.*;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.MultipartBody;

import org.pyload.android.openapi.model.AccountInfo;
import org.pyload.android.openapi.model.ApiAddFilesPostRequest;
import org.pyload.android.openapi.model.ApiAddPackagePostRequest;
import org.pyload.android.openapi.model.ApiCheckAndAddPackagesPostRequest;
import org.pyload.android.openapi.model.ApiCheckOnlineStatusPostRequest;
import org.pyload.android.openapi.model.ApiCheckUrlsPostRequest;
import org.pyload.android.openapi.model.ApiDeleteFilesPostRequest;
import org.pyload.android.openapi.model.ApiDeletePackagesPostRequest;
import org.pyload.android.openapi.model.ApiGeneratePackagesPostRequest;
import org.pyload.android.openapi.model.ApiIsAuthorizedPostRequest;
import org.pyload.android.openapi.model.ApiMoveFilesPostRequest;
import org.pyload.android.openapi.model.ApiParseUrlsPostRequest;
import org.pyload.android.openapi.model.ApiServiceCallPostRequest;
import org.pyload.android.openapi.model.ApiSetConfigValuePostRequest;
import org.pyload.android.openapi.model.ApiSetPackageDataPostRequest;
import org.pyload.android.openapi.model.ApiStopDownloadsPostRequest;
import org.pyload.android.openapi.model.ApiUpdateAccountPostRequest;
import org.pyload.android.openapi.model.CaptchaTask;
import org.pyload.android.openapi.model.ConfigSection;
import org.pyload.android.openapi.model.Destination;
import org.pyload.android.openapi.model.DownloadInfo;
import org.pyload.android.openapi.model.EventInfo;
import java.io.File;
import org.pyload.android.openapi.model.FileData;
import org.pyload.android.openapi.model.OldUserData;
import org.pyload.android.openapi.model.OnlineCheck;
import org.pyload.android.openapi.model.PackageData;
import org.pyload.android.openapi.model.ServerStatus;
import org.pyload.android.openapi.model.UserData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PyLoadRestApi {
  /**
   * Adds files to specific package.
   * Adds files to specific package.
   * @param apiAddFilesPostRequest  (optional)
   * @return Call&lt;Void&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @POST("api/add_files")
  Call<Void> apiAddFilesPost(
    @retrofit2.http.Body ApiAddFilesPostRequest apiAddFilesPostRequest
  );

  /**
   * Adds a package, with links to desired destination.
   * Adds a package, with links to desired destination.
   * @param apiAddPackagePostRequest  (optional)
   * @return Call&lt;Integer&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @POST("api/add_package")
  Call<Integer> apiAddPackagePost(
    @retrofit2.http.Body ApiAddPackagePostRequest apiAddPackagePostRequest
  );

  /**
   * creates new user login.
   * creates new user login.
   * @param user  (required)
   * @param newpw  (required)
   * @param role  (optional, default to 0)
   * @param perms  (optional, default to 0)
   * @return Call&lt;Boolean&gt;
   */
  @POST("api/add_user")
  Call<Boolean> apiAddUserPost(
    @retrofit2.http.Query("user") String user, @retrofit2.http.Query("newpw") String newpw, @retrofit2.http.Query("role") Integer role, @retrofit2.http.Query("perms") Integer perms
  );

  /**
   * changes password for specific user.
   * changes password for specific user.
   * @param user  (required)
   * @param oldpw  (required)
   * @param newpw  (required)
   * @return Call&lt;Boolean&gt;
   */
  @POST("api/change_password")
  Call<Boolean> apiChangePasswordPost(
    @retrofit2.http.Query("user") String user, @retrofit2.http.Query("oldpw") String oldpw, @retrofit2.http.Query("newpw") String newpw
  );

  /**
   * Checks online status, retrieves names, and will add packages. Because of these packages are not added immediately, only for internal use.
   * Checks online status, retrieves names, and will add packages. Because of these packages are not added immediately, only for internal use.
   * @param apiCheckAndAddPackagesPostRequest  (optional)
   * @return Call&lt;Void&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @POST("api/check_and_add_packages")
  Call<Void> apiCheckAndAddPackagesPost(
    @retrofit2.http.Body ApiCheckAndAddPackagesPostRequest apiCheckAndAddPackagesPostRequest
  );

  /**
   * Check authentication and returns details.
   * Check authentication and returns details.
   * @param username  (required)
   * @param password  (required)
   * @return Call&lt;Map&lt;String, Object&gt;&gt;
   */
  @GET("api/check_auth")
  Call<Map<String, Object>> apiCheckAuthGet(
    @retrofit2.http.Query("username") String username, @retrofit2.http.Query("password") String password
  );

  /**
   * checks online status of urls and a submitted container file.
   * checks online status of urls and a submitted container file.
   * @param urls list of urls (required)
   * @param container container file name (required)
   * @param data file content (required)
   * @return Call&lt;OnlineCheck&gt;
   */
  @retrofit2.http.Multipart
  @POST("api/check_online_status_container")
  Call<OnlineCheck> apiCheckOnlineStatusContainerPost(
    @retrofit2.http.Part("urls") List<String> urls, @retrofit2.http.Part("container") String container, @retrofit2.http.Part MultipartBody.Part data
  );

  /**
   * Initiates online status check.
   * Initiates online status check.
   * @param apiCheckOnlineStatusPostRequest  (optional)
   * @return Call&lt;OnlineCheck&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @POST("api/check_online_status")
  Call<OnlineCheck> apiCheckOnlineStatusPost(
    @retrofit2.http.Body ApiCheckOnlineStatusPostRequest apiCheckOnlineStatusPostRequest
  );

  /**
   * Gets urls and returns plugin name mapped to list of matched urls.
   * Gets urls and returns plugin name mapped to list of matched urls.
   * @param apiCheckUrlsPostRequest  (optional)
   * @return Call&lt;Map&lt;String, List&lt;String&gt;&gt;&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @POST("api/check_urls")
  Call<Map<String, List<String>>> apiCheckUrlsPost(
    @retrofit2.http.Body ApiCheckUrlsPostRequest apiCheckUrlsPostRequest
  );

  /**
   * Deletes several file entries from pyload.
   * Deletes several file entries from pyload.
   * @param apiDeleteFilesPostRequest  (optional)
   * @return Call&lt;Void&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @POST("api/delete_files")
  Call<Void> apiDeleteFilesPost(
    @retrofit2.http.Body ApiDeleteFilesPostRequest apiDeleteFilesPostRequest
  );

  /**
   * Deletes all finished files and completely finished packages.
   * Deletes all finished files and completely finished packages.
   * @return Call&lt;List&lt;Integer&gt;&gt;
   */
  @POST("api/delete_finished")
  Call<List<Integer>> apiDeleteFinishedPost();
    

  /**
   * Deletes packages and containing links.
   * Deletes packages and containing links.
   * @param apiDeletePackagesPostRequest  (optional)
   * @return Call&lt;Void&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @POST("api/delete_packages")
  Call<Void> apiDeletePackagesPost(
    @retrofit2.http.Body ApiDeletePackagesPostRequest apiDeletePackagesPostRequest
  );

  /**
   * Available free space at download directory in bytes.
   * Available free space at download directory in bytes.
   * @return Call&lt;Integer&gt;
   */
  @GET("api/free_space")
  Call<Integer> apiFreeSpaceGet();
    

  /**
   * Generates and add packages.
   * Generates and add packages.
   * @param apiCheckAndAddPackagesPostRequest  (optional)
   * @return Call&lt;List&lt;Integer&gt;&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @POST("api/generate_and_add_packages")
  Call<List<Integer>> apiGenerateAndAddPackagesPost(
    @retrofit2.http.Body ApiCheckAndAddPackagesPostRequest apiCheckAndAddPackagesPostRequest
  );

  /**
   * Parses links, generates packages names from urls.
   * Parses links, generates packages names from urls.
   * @param apiGeneratePackagesPostRequest  (optional)
   * @return Call&lt;Map&lt;String, List&lt;String&gt;&gt;&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @POST("api/generate_packages")
  Call<Map<String, List<String>>> apiGeneratePackagesPost(
    @retrofit2.http.Body ApiGeneratePackagesPostRequest apiGeneratePackagesPostRequest
  );

  /**
   * All available account types.
   * All available account types.
   * @return Call&lt;List&lt;String&gt;&gt;
   */
  @GET("api/get_account_types")
  Call<List<String>> apiGetAccountTypesGet();
    

  /**
   * Get information about all entered accounts.
   * Get information about all entered accounts.
   * @param refresh reload account info (required)
   * @return Call&lt;List&lt;AccountInfo&gt;&gt;
   */
  @GET("api/get_accounts")
  Call<List<AccountInfo>> apiGetAccountsGet(
    @retrofit2.http.Query("refresh") Boolean refresh
  );

  /**
   * Returns all information stored by addon plugins. Values are always strings.
   * Returns all information stored by addon plugins. Values are always strings.
   * @return Call&lt;Map&lt;String, Map&lt;String, String&gt;&gt;&gt;
   */
  @GET("api/get_all_info")
  Call<Map<String, Map<String, String>>> apiGetAllInfoGet();
    

  /**
   * returns all known user and info.
   * returns all known user and info.
   * @return Call&lt;Map&lt;String, OldUserData&gt;&gt;
   */
  @GET("api/getAllUserData")
  Call<Map<String, OldUserData>> apiGetAllUserDataGet();
    

  /**
   * returns all known user and info.
   * returns all known user and info.
   * @return Call&lt;Map&lt;String, UserData&gt;&gt;
   */
  @GET("api/get_all_userdata")
  Call<Map<String, UserData>> apiGetAllUserdataGet();
    

  /**
   * No documentation available
   * No documentation available
   * @return Call&lt;String&gt;
   */
  @GET("api/get_cachedir")
  Call<String> apiGetCachedirGet();
    

  /**
   * Returns a captcha task.
   * Returns a captcha task.
   * @param exclusive unused (optional, default to false)
   * @return Call&lt;CaptchaTask&gt;
   */
  @GET("api/get_captcha_task")
  Call<CaptchaTask> apiGetCaptchaTaskGet(
    @retrofit2.http.Query("exclusive") Boolean exclusive
  );

  /**
   * Get information about captcha task.
   * Get information about captcha task.
   * @param tid task id (required)
   * @return Call&lt;String&gt;
   */
  @GET("api/get_captcha_task_status")
  Call<String> apiGetCaptchaTaskStatusGet(
    @retrofit2.http.Query("tid") Integer tid
  );

  /**
   * same as &#x60;get_queue_data&#x60; for collector.
   * same as &#x60;get_queue_data&#x60; for collector.
   * @return Call&lt;List&lt;PackageData&gt;&gt;
   */
  @GET("api/get_collector_data")
  Call<List<PackageData>> apiGetCollectorDataGet();
    

  /**
   * same as &#x60;get_queue&#x60; for collector.
   * same as &#x60;get_queue&#x60; for collector.
   * @return Call&lt;List&lt;PackageData&gt;&gt;
   */
  @GET("api/get_collector")
  Call<List<PackageData>> apiGetCollectorGet();
    

  /**
   * Retrieves complete config in dict format, not for RPC.
   * Retrieves complete config in dict format, not for RPC.
   * @return Call&lt;Map&lt;String, Object&gt;&gt;
   */
  @GET("api/get_config_dict")
  Call<Map<String, Object>> apiGetConfigDictGet();
    

  /**
   * Retrieves complete config of core.
   * Retrieves complete config of core.
   * @return Call&lt;Map&lt;String, ConfigSection&gt;&gt;
   */
  @GET("api/get_config")
  Call<Map<String, ConfigSection>> apiGetConfigGet();
    

  /**
   * Retrieve config value.
   * Retrieve config value.
   * @param category name of category, or plugin (required)
   * @param option config option (required)
   * @param section &#39;plugin&#39; or &#39;core&#39; (optional, default to core)
   * @return Call&lt;Object&gt;
   */
  @GET("api/get_config_value")
  Call<Object> apiGetConfigValueGet(
    @retrofit2.http.Query("category") String category, @retrofit2.http.Query("option") String option, @retrofit2.http.Query("section") String section
  );

  /**
   * Lists occurred events, may be affected to changes in the future.
   * Lists occurred events, may be affected to changes in the future.
   * @param uuid  (required)
   * @return Call&lt;List&lt;EventInfo&gt;&gt;
   */
  @GET("api/get_events")
  Call<List<EventInfo>> apiGetEventsGet(
    @retrofit2.http.Query("uuid") String uuid
  );

  /**
   * Get complete information about a specific file.
   * Get complete information about a specific file.
   * @param fileId file id (required)
   * @return Call&lt;FileData&gt;
   */
  @GET("api/get_file_data")
  Call<FileData> apiGetFileDataGet(
    @retrofit2.http.Query("file_id") Integer fileId
  );

  /**
   * Information about file order within package.
   * Information about file order within package.
   * @param packageId  (required)
   * @return Call&lt;Map&lt;String, Integer&gt;&gt;
   */
  @GET("api/get_file_order")
  Call<Map<String, Integer>> apiGetFileOrderGet(
    @retrofit2.http.Query("package_id") Integer packageId
  );

  /**
   * Returns information stored by a specific plugin.
   * Returns information stored by a specific plugin.
   * @param plugin pluginname (required)
   * @return Call&lt;Map&lt;String, String&gt;&gt;
   */
  @GET("api/get_info_by_plugin")
  Call<Map<String, String>> apiGetInfoByPluginGet(
    @retrofit2.http.Query("plugin") String plugin
  );

  /**
   * Returns most recent log entries.
   * Returns most recent log entries.
   * @param offset line offset (optional, default to 0)
   * @return Call&lt;List&lt;String&gt;&gt;
   */
  @GET("api/get_log")
  Call<List<String>> apiGetLogGet(
    @retrofit2.http.Query("offset") Integer offset
  );

  /**
   * Returns complete information about package, and included files.
   * Returns complete information about package, and included files.
   * @param packageId package id (required)
   * @return Call&lt;PackageData&gt;
   */
  @GET("api/get_package_data")
  Call<PackageData> apiGetPackageDataGet(
    @retrofit2.http.Query("package_id") Integer packageId
  );

  /**
   * Returns information about package, without detailed information about containing files.
   * Returns information about package, without detailed information about containing files.
   * @param packageId package id (required)
   * @return Call&lt;PackageData&gt;
   */
  @GET("api/get_package_info")
  Call<PackageData> apiGetPackageInfoGet(
    @retrofit2.http.Query("package_id") Integer packageId
  );

  /**
   * Returns information about package order.
   * Returns information about package order.
   * @param destination &#x60;Destination&#x60; (required)
   * @return Call&lt;Map&lt;String, Integer&gt;&gt;
   */
  @GET("api/get_package_order")
  Call<Map<String, Integer>> apiGetPackageOrderGet(
    @retrofit2.http.Query("destination") Destination destination
  );

  /**
   * Plugin config as dict, not for RPC.
   * Plugin config as dict, not for RPC.
   * @return Call&lt;Map&lt;String, Object&gt;&gt;
   */
  @GET("api/get_plugin_config_dict")
  Call<Map<String, Object>> apiGetPluginConfigDictGet();
    

  /**
   * Retrieves complete config for all plugins.
   * Retrieves complete config for all plugins.
   * @return Call&lt;Map&lt;String, ConfigSection&gt;&gt;
   */
  @GET("api/get_plugin_config")
  Call<Map<String, ConfigSection>> apiGetPluginConfigGet();
    

  /**
   * Return complete data about everything in queue, this is very expensive use it sparely. See &#x60;get_queue&#x60; for alternative.
   * Return complete data about everything in queue, this is very expensive use it sparely. See &#x60;get_queue&#x60; for alternative.
   * @return Call&lt;List&lt;PackageData&gt;&gt;
   */
  @GET("api/get_queue_data")
  Call<List<PackageData>> apiGetQueueDataGet();
    

  /**
   * Returns info about queue and packages, **not** about files, see &#x60;get_queue_data&#x60; or &#x60;get_package_data&#x60; instead.
   * Returns info about queue and packages, **not** about files, see &#x60;get_queue_data&#x60; or &#x60;get_package_data&#x60; instead.
   * @return Call&lt;List&lt;PackageData&gt;&gt;
   */
  @GET("api/get_queue")
  Call<List<PackageData>> apiGetQueueGet();
    

  /**
   * pyLoad Core version.
   * pyLoad Core version.
   * @return Call&lt;String&gt;
   */
  @GET("api/get_server_version")
  Call<String> apiGetServerVersionGet();
    

  /**
   * A dict of available services, these can be defined by addon plugins.
   * A dict of available services, these can be defined by addon plugins.
   * @return Call&lt;Map&lt;String, Map&lt;String, String&gt;&gt;&gt;
   */
  @GET("api/get_services")
  Call<Map<String, Map<String, String>>> apiGetServicesGet();
    

  /**
   * similar to &#x60;check_auth&#x60; but returns UserData type.
   * similar to &#x60;check_auth&#x60; but returns UserData type.
   * @param username  (required)
   * @param password  (required)
   * @return Call&lt;OldUserData&gt;
   */
  @GET("api/getUserData")
  Call<OldUserData> apiGetUserDataGet(
    @retrofit2.http.Query("username") String username, @retrofit2.http.Query("password") String password
  );

  /**
   * similar to &#x60;check_auth&#x60; but returns UserData pe.
   * similar to &#x60;check_auth&#x60; but returns UserData pe.
   * @param username  (required)
   * @param password  (required)
   * @return Call&lt;UserData&gt;
   */
  @GET("api/get_userdata")
  Call<UserData> apiGetUserdataGet(
    @retrofit2.http.Query("username") String username, @retrofit2.http.Query("password") String password
  );

  /**
   * No documentation available
   * No documentation available
   * @return Call&lt;String&gt;
   */
  @GET("api/get_userdir")
  Call<String> apiGetUserdirGet();
    

  /**
   * Checks whether a service is available.
   * Checks whether a service is available.
   * @param plugin  (required)
   * @param funcName  (required)
   * @return Call&lt;Boolean&gt;
   */
  @GET("api/has_service")
  Call<Boolean> apiHasServiceGet(
    @retrofit2.http.Query("plugin") String plugin, @retrofit2.http.Query("func_name") String funcName
  );

  /**
   * checks if the user is authorized for specific method.
   * checks if the user is authorized for specific method.
   * @param apiIsAuthorizedPostRequest  (optional)
   * @return Call&lt;Boolean&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @POST("api/is_authorized")
  Call<Boolean> apiIsAuthorizedPost(
    @retrofit2.http.Body ApiIsAuthorizedPostRequest apiIsAuthorizedPostRequest
  );

  /**
   * Indicates whether a captcha task is available.
   * Indicates whether a captcha task is available.
   * @return Call&lt;Boolean&gt;
   */
  @GET("api/is_captcha_waiting")
  Call<Boolean> apiIsCaptchaWaitingGet();
    

  /**
   * Checks if pyload will start new downloads according to time in config.
   * Checks if pyload will start new downloads according to time in config.
   * @return Call&lt;Boolean&gt;
   */
  @GET("api/is_time_download")
  Call<Boolean> apiIsTimeDownloadGet();
    

  /**
   * Checks if pyload will try to make a reconnect.
   * Checks if pyload will try to make a reconnect.
   * @return Call&lt;Boolean&gt;
   */
  @GET("api/is_time_reconnect")
  Call<Boolean> apiIsTimeReconnectGet();
    

  /**
   * Clean way to quit pyLoad.
   * Clean way to quit pyLoad.
   * @return Call&lt;Void&gt;
   */
  @POST("api/kill")
  Call<Void> apiKillPost();
    

  /**
   * Move multiple files to another package.
   * Move multiple files to another package.
   * @param apiMoveFilesPostRequest  (optional)
   * @return Call&lt;Void&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @POST("api/move_files")
  Call<Void> apiMoveFilesPost(
    @retrofit2.http.Body ApiMoveFilesPostRequest apiMoveFilesPostRequest
  );

  /**
   * Set a new package location.
   * Set a new package location.
   * @param destination &#x60;Destination&#x60; (required)
   * @param packageId package id (required)
   * @return Call&lt;Void&gt;
   */
  @POST("api/move_package")
  Call<Void> apiMovePackagePost(
    @retrofit2.http.Query("destination") Destination destination, @retrofit2.http.Query("package_id") Integer packageId
  );

  /**
   * Gives a new position to a file within its package.
   * Gives a new position to a file within its package.
   * @param fileId file id (required)
   * @param position  (required)
   * @return Call&lt;Void&gt;
   */
  @POST("api/order_file")
  Call<Void> apiOrderFilePost(
    @retrofit2.http.Query("file_id") Integer fileId, @retrofit2.http.Query("position") Integer position
  );

  /**
   * Gives a package a new position.
   * Gives a package a new position.
   * @param packageId package id (required)
   * @param position  (required)
   * @return Call&lt;Void&gt;
   */
  @POST("api/order_package")
  Call<Void> apiOrderPackagePost(
    @retrofit2.http.Query("package_id") Integer packageId, @retrofit2.http.Query("position") Integer position
  );

  /**
   * Parses html content or any arbitrary text for links and returns result of &#x60;check_urls&#x60;
   * Parses html content or any arbitrary text for links and returns result of &#x60;check_urls&#x60;
   * @param apiParseUrlsPostRequest  (optional)
   * @return Call&lt;Map&lt;String, List&lt;String&gt;&gt;&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @POST("api/parse_urls")
  Call<Map<String, List<String>>> apiParseUrlsPost(
    @retrofit2.http.Body ApiParseUrlsPostRequest apiParseUrlsPostRequest
  );

  /**
   * Pause server: It won&#39;t start any new downloads, but nothing gets aborted.
   * Pause server: It won&#39;t start any new downloads, but nothing gets aborted.
   * @return Call&lt;Void&gt;
   */
  @POST("api/pause_server")
  Call<Void> apiPauseServerPost();
    

  /**
   * Polls the result available for ResultID.
   * Polls the result available for ResultID.
   * @param rid &#x60;ResultID&#x60; (required)
   * @return Call&lt;OnlineCheck&gt;
   */
  @GET("api/poll_results")
  Call<OnlineCheck> apiPollResultsGet(
    @retrofit2.http.Query("rid") Integer rid
  );

  /**
   * Moves package from Queue to Collector.
   * Moves package from Queue to Collector.
   * @param packageId package id (required)
   * @return Call&lt;Void&gt;
   */
  @POST("api/pull_from_queue")
  Call<Void> apiPullFromQueuePost(
    @retrofit2.http.Query("package_id") Integer packageId
  );

  /**
   * Moves package from Collector to Queue.
   * Moves package from Collector to Queue.
   * @param packageId package id (required)
   * @return Call&lt;Void&gt;
   */
  @POST("api/push_to_queue")
  Call<Void> apiPushToQueuePost(
    @retrofit2.http.Query("package_id") Integer packageId
  );

  /**
   * Probes online status of all files in a package, also a default action when package is added.
   * Probes online status of all files in a package, also a default action when package is added.
   * @param packageId  (required)
   * @return Call&lt;Void&gt;
   */
  @POST("api/recheck_package")
  Call<Void> apiRecheckPackagePost(
    @retrofit2.http.Query("package_id") Integer packageId
  );

  /**
   * Remove account from pyload.
   * Remove account from pyload.
   * @param plugin pluginname (required)
   * @param account accountname (required)
   * @return Call&lt;Void&gt;
   */
  @POST("api/remove_account")
  Call<Void> apiRemoveAccountPost(
    @retrofit2.http.Query("plugin") String plugin, @retrofit2.http.Query("account") String account
  );

  /**
   * deletes a user login.
   * deletes a user login.
   * @param user  (required)
   * @return Call&lt;Boolean&gt;
   */
  @POST("api/remove_user")
  Call<Boolean> apiRemoveUserPost(
    @retrofit2.http.Query("user") String user
  );

  /**
   * Restarts all failed failes.
   * Restarts all failed failes.
   * @return Call&lt;Void&gt;
   */
  @POST("api/restart_failed")
  Call<Void> apiRestartFailedPost();
    

  /**
   * Resets file status, so it will be downloaded again.
   * Resets file status, so it will be downloaded again.
   * @param fileId  file id (required)
   * @return Call&lt;Void&gt;
   */
  @POST("api/restart_file")
  Call<Void> apiRestartFilePost(
    @retrofit2.http.Query("file_id") Integer fileId
  );

  /**
   * Restarts a package, resets every containing files.
   * Restarts a package, resets every containing files.
   * @param packageId package id (required)
   * @return Call&lt;Void&gt;
   */
  @POST("api/restart_package")
  Call<Void> apiRestartPackagePost(
    @retrofit2.http.Query("package_id") Integer packageId
  );

  /**
   * Restart pyload core.
   * Restart pyload core.
   * @return Call&lt;Void&gt;
   */
  @POST("api/restart")
  Call<Void> apiRestartPost();
    

  /**
   * Calls a service (a method in addon plugin).
   * Calls a service (a method in addon plugin).
   * @param apiServiceCallPostRequest  (optional)
   * @return Call&lt;String&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @POST("api/service_call")
  Call<String> apiServiceCallPost(
    @retrofit2.http.Body ApiServiceCallPostRequest apiServiceCallPostRequest
  );

  /**
   * Set result for a captcha task.
   * Set result for a captcha task.
   * @param tid task id (required)
   * @param result captcha result (required)
   * @return Call&lt;Void&gt;
   */
  @POST("api/set_captcha_result")
  Call<Void> apiSetCaptchaResultPost(
    @retrofit2.http.Query("tid") Integer tid, @retrofit2.http.Query("result") String result
  );

  /**
   * Set new config value.
   * Set new config value.
   * @param apiSetConfigValuePostRequest  (optional)
   * @return Call&lt;Void&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @POST("api/set_config_value")
  Call<Void> apiSetConfigValuePost(
    @retrofit2.http.Body ApiSetConfigValuePostRequest apiSetConfigValuePostRequest
  );

  /**
   * Allows to modify several package attributes.
   * Allows to modify several package attributes.
   * @param apiSetPackageDataPostRequest  (optional)
   * @return Call&lt;Void&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @POST("api/set_package_data")
  Call<Void> apiSetPackageDataPost(
    @retrofit2.http.Body ApiSetPackageDataPostRequest apiSetPackageDataPostRequest
  );

  /**
   * Renames a package.
   * Renames a package.
   * @param packageId package id (required)
   * @param name new package name (required)
   * @return Call&lt;Void&gt;
   */
  @POST("api/set_package_name")
  Call<Void> apiSetPackageNamePost(
    @retrofit2.http.Query("package_id") Integer packageId, @retrofit2.http.Query("name") String name
  );

  /**
   * No documentation available
   * No documentation available
   * @param user  (required)
   * @param permission  (required)
   * @param role  (required)
   * @return Call&lt;Void&gt;
   */
  @POST("api/set_user_permission")
  Call<Void> apiSetUserPermissionPost(
    @retrofit2.http.Query("user") String user, @retrofit2.http.Query("permission") Integer permission, @retrofit2.http.Query("role") Integer role
  );

  /**
   * Status of all currently running downloads.
   * Status of all currently running downloads.
   * @return Call&lt;List&lt;DownloadInfo&gt;&gt;
   */
  @GET("api/status_downloads")
  Call<List<DownloadInfo>> apiStatusDownloadsGet();
    

  /**
   * Some general information about the current status of pyLoad.
   * Some general information about the current status of pyLoad.
   * @return Call&lt;ServerStatus&gt;
   */
  @GET("api/status_server")
  Call<ServerStatus> apiStatusServerGet();
    

  /**
   * Aborts all running downloads.
   * Aborts all running downloads.
   * @return Call&lt;Void&gt;
   */
  @POST("api/stop_all_downloads")
  Call<Void> apiStopAllDownloadsPost();
    

  /**
   * Aborts specific downloads.
   * Aborts specific downloads.
   * @param apiStopDownloadsPostRequest  (optional)
   * @return Call&lt;Void&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @POST("api/stop_downloads")
  Call<Void> apiStopDownloadsPost(
    @retrofit2.http.Body ApiStopDownloadsPostRequest apiStopDownloadsPostRequest
  );

  /**
   * Toggle pause state.
   * Toggle pause state.
   * @return Call&lt;Boolean&gt;
   */
  @POST("api/toggle_pause")
  Call<Boolean> apiTogglePausePost();
    

  /**
   * Toggle proxy activation.
   * Toggle proxy activation.
   * @return Call&lt;Boolean&gt;
   */
  @POST("api/toggle_proxy")
  Call<Boolean> apiToggleProxyPost();
    

  /**
   * Toggle reconnect activation.
   * Toggle reconnect activation.
   * @return Call&lt;Boolean&gt;
   */
  @POST("api/toggle_reconnect")
  Call<Boolean> apiToggleReconnectPost();
    

  /**
   * Unpause server: New Downloads will be started.
   * Unpause server: New Downloads will be started.
   * @return Call&lt;Void&gt;
   */
  @POST("api/unpause_server")
  Call<Void> apiUnpauseServerPost();
    

  /**
   * Changes pw/options for specific account.
   * Changes pw/options for specific account.
   * @param apiUpdateAccountPostRequest  (optional)
   * @return Call&lt;Void&gt;
   */
  @Headers({
    "Content-Type:application/json"
  })
  @POST("api/update_account")
  Call<Void> apiUpdateAccountPost(
    @retrofit2.http.Body ApiUpdateAccountPostRequest apiUpdateAccountPostRequest
  );

  /**
   * Uploads and adds a container file to pyLoad.
   * Uploads and adds a container file to pyLoad.
   * @param filename file name - extension is important, so it can correctly decrypt (required)
   * @param data file content (required)
   * @param dest &#x60;Destination&#x60; (optional, default to 0)
   * @return Call&lt;Void&gt;
   */
  @retrofit2.http.Multipart
  @POST("api/upload_container")
  Call<Void> apiUploadContainerPost(
    @retrofit2.http.Part("filename") String filename, @retrofit2.http.Part MultipartBody.Part data, @retrofit2.http.Part("dest") Destination dest
  );

  /**
   * Check if a user actually exists in the database.
   * Check if a user actually exists in the database.
   * @param username  (required)
   * @return Call&lt;Boolean&gt;
   */
  @GET("api/user_exists")
  Call<Boolean> apiUserExistsGet(
    @retrofit2.http.Query("username") String username
  );

}
