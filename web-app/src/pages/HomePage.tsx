import useAuth from "@/hooks/useAuth";
import useLogout from "@/hooks/useLogout";
import useRefreshToken from "@/hooks/useRefresh";
import { useEffect } from "react";

const HomePage = () => {
  const { token } = useAuth();
  const logout = useLogout();
  const refresh = useRefreshToken();

  async function signOut(e: any) {
    e.preventDefault();
    await logout();
    // window.location.reload();
  }

  useEffect(() => {
    const effect = async () => {
      if (token === undefined) {
        await refresh();
      }
    };
    effect();
  }, []);

  return (
    <div className="h-screen">
      <div>
        {token != undefined ? (
          <>
            <p>Logged In!</p>
            <a onClick={signOut}>Log out</a>
          </>
        ) : (
          <a href="/auth">Log In</a>
        )}
      </div>
      <p>Welcome!</p>
    </div>
  );
};

export default HomePage;
