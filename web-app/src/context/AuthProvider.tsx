import { createContext, useState } from "react";

type AuthContextType = {
  token: any;
  setToken: React.Dispatch<React.SetStateAction<any>>;
};

export const AuthContext = createContext({} as AuthContextType);

export const AuthProvider = ({ children }: any) => {
  const [token, setToken] = useState<string>();

  return (
    <AuthContext.Provider value={{ token, setToken }}>
      {children}
    </AuthContext.Provider>
  );
};

export default AuthProvider;
