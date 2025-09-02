import axios from "../api/axios";
import useAuth from "./useAuth";

const useLogout = () => {
  const { setToken } = useAuth();

  const logout = async () => {
    setToken(undefined);
    try {
      await axios.post("/v1/auth/logout", {}, { withCredentials: true });
    } catch (e) {
      console.log(e);
    }
  };

  return logout;
};

export default useLogout;
