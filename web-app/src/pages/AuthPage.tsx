import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "../components/ui/card";
import { useState } from "react";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import z from "zod";
import axios from "../api/axios";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import LPAP from "./components/LPAP";
import { useLocation, useNavigate } from "react-router-dom";
import useAuth from "@/hooks/useAuth";

const AuthForm = z.object({
  email: z.email("Please enter a valid email"),
  password: z
    .string()
    .min(7, "Please enter a password with at least 7 characters"),
});

const AuthPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { setToken } = useAuth();

  const from = location.state?.from?.pathname || "/";

  const [loginState, setLoginState] = useState(false);
  const [error, setError] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string>("");
  const [email, setEmail] = useState<string>("");
  const [password, setPassword] = useState<string>("");
  const [signUpSubmitted, setSignUpSubmitted] = useState(false);

  function validateForm(e: any) {
    try {
      e.preventDefault();
      setError(false);
      const formData = { email: email, password: password };
      // console.log(formData);
      AuthForm.parse({ email: email, password: password });

      if (loginState) {
        axios
          .post("/v1/auth/login", formData)
          .then((response) => {
            setToken(response.data.accessToken);
            navigate(from, { replace: true });
          })
          .catch(() => {
            setError(true);
            setErrorMessage(
              "Something went wrong went logging in. Please try again or contact support"
            );
          });
      } else {
        axios
          .post("/v1/auth/register", formData)
          .then(() => {
            setSignUpSubmitted(true);
          })
          .catch(() => {
            setError(true);
            setErrorMessage(
              "Unable to register this user. Please try again or contact support."
            );
          });
      }
    } catch (e) {
      if (e instanceof z.ZodError) {
        setError(true);
        setErrorMessage(z.prettifyError(e));
      }
    }
  }

  return (
    <div className="animate-fadeIn">
      <LPAP />
      <Card className="w-[365px]">
        <CardHeader>
          <CardTitle>Welcome to LPAP!</CardTitle>
          <CardDescription>Please log in or sign up here</CardDescription>
        </CardHeader>
        <CardContent>
          {signUpSubmitted ? (
            <p>
              You have been signed up! We have sent you an email to verify your
              code.
            </p>
          ) : (
            <form onSubmit={validateForm}>
              <div className="flex flex-col gap-6">
                {error && (
                  <Alert variant="destructive">
                    <AlertTitle className="block">
                      One or more errors occured submitting this form:
                    </AlertTitle>
                    <AlertDescription>{errorMessage}</AlertDescription>
                  </Alert>
                )}
                <div className="grid gap-2">
                  <Label htmlFor="email">Email</Label>
                  <Input
                    onChange={(e) => setEmail(e.target.value)}
                    id="email"
                    type="email"
                    placeholder="email@example.com"
                    required
                    value={email}
                  />
                </div>
                <div className="grid gap-2">
                  <Label htmlFor="password">Password</Label>
                  <Input
                    onChange={(e) => setPassword(e.target.value)}
                    id="password"
                    type="password"
                    required
                    value={password}
                  />
                </div>
                <Button type="submit">
                  {loginState ? "Log In" : "Sign Up"}
                </Button>
              </div>
            </form>
          )}
        </CardContent>
        <CardFooter>
          {!signUpSubmitted && (
            <p>
              {loginState
                ? "Don't have an account? Please "
                : "Already have an account? Please "}{" "}
              <a
                style={{ textDecoration: "underline" }}
                className="cursor-pointer"
                onClick={() => setLoginState((x) => !x)}
              >
                {loginState ? "Sign Up" : "Log In"}
              </a>{" "}
              here
            </p>
          )}
        </CardFooter>
      </Card>
    </div>
  );
};

export default AuthPage;
