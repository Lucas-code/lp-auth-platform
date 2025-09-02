import { useEffect } from "react";
import { authedCall } from "../api/axios";
import useAuth from "./useAuth";
import useRefreshToken from "./useRefresh";

const useAuthedCall = () => {
  const refresh = useRefreshToken();
  const { token } = useAuth();

  useEffect(() => {
    const requestIntercept = authedCall.interceptors.request.use(
      (config) => {
        if (!config.headers["Authorization"]) {
          config.headers["Authorization"] = `Bearer ${token}`;
        }

        return config;
      },
      (error) => Promise.reject(error)
    );

    const responseIntercept = authedCall.interceptors.response.use(
      (response) => response,
      async (error) => {
        const prevRequest = error?.config;
        if (error?.response?.status === 403 && !prevRequest?.sent) {
          prevRequest.sent = true;
          const newAccessToken = await refresh();
          prevRequest.headers["Autorization"] = `Bearer ${newAccessToken}`;
          return authedCall;
        }

        return Promise.reject(error);
      }
    );

    return () => {
      authedCall.interceptors.request.eject(requestIntercept);
      authedCall.interceptors.response.eject(responseIntercept);
    };
  }, [auth, refresh]);

  return authedCall;
};

export default useAuthedCall;
