import axios from "../api/axios";
import useAuth from "./useAuth";

const useRefreshToken = () => {
  const { setToken } = useAuth();

  const refresh = async () => {
    try {
      const response = await axios.get("/v1/auth/refresh-token", {
        withCredentials: true,
      });

      setToken(response.data.accessToken);

      return response.data.accessToken;
    } catch (e) {
      console.log(e);
    }
  };

  return refresh;
};

export default useRefreshToken;
