import { useNavigate, useParams } from "react-router-dom";
import LPAP from "./components/LPAP";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { useEffect, useState } from "react";
import axios from "@/api/axios";
import { Alert, AlertTitle } from "@/components/ui/alert";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import useAuth from "@/hooks/useAuth";

const Verify = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { setToken } = useAuth();

  const [loading, setLoading] = useState(false);
  const [accountEnabled, setAccountEnabled] = useState(false);
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [verificationCode, setVerificationCode] = useState<number>();
  const [verificationCodeResent, setVerificationCodeResent] = useState(false);

  useEffect(() => {
    axios.get("/v1/auth/userEnabled?id=" + id).then((response) => {
      setAccountEnabled(response.data);
    });
  }, []);

  function onSubmit(e: any) {
    e.preventDefault();
    setLoading(true);
    axios
      .post("/v1/auth/verify?id=" + id, { verificationCode })
      .then((response) => {
        setToken(response.data.accessToken);
        //   navigate("/");
        setSuccess(true);
      })
      .catch(() => {
        setError(true);
        setErrorMessage("Failed to activate account.");
      })
      .finally(() => {
        setLoading(false);
      });
  }

  function resendVerificationEmail(e: any) {
    e.preventDefault();
    // setLoading(true);
    axios
      .post("/v1/auth/resendVerification?id=" + id)
      .then(() => {
        setVerificationCodeResent(true);
      })
      .catch(() => {
        setError(true);
        setErrorMessage(
          "Failed to resend a new verification code. Please try again or contact support"
        );
      });
    // .finally(() => {
    //   setLoading(false);
    // })
  }

  return (
    <div>
      <LPAP />
      <Card>
        <CardHeader>
          <CardTitle>Activate Account</CardTitle>
          <CardDescription>
            Please enter the correct verification code to activate this account
          </CardDescription>
        </CardHeader>
        <CardContent>
          {loading ? (
            <p>Loading...</p>
          ) : accountEnabled ? (
            <div className="flex flex-col gap-6">
              <p>This account has already been activated.</p>
              <a onClick={() => navigate("/")}>To Home Page</a>
            </div>
          ) : success ? (
            <div className="flex flex-col gap-6">
              <p>This account has successfully been activated!</p>
              <a onClick={() => navigate("/")}>To Home Page</a>
            </div>
          ) : (
            <form onSubmit={onSubmit}>
              <div className="flex flex-col gap-6">
                {error ? (
                  <Alert variant="destructive">
                    <AlertTitle className="block">{errorMessage}</AlertTitle>
                  </Alert>
                ) : (
                  verificationCodeResent && (
                    <p className="text-green-200">
                      Verification code has been resent!
                    </p>
                  )
                )}
                <div className="grid gap-2">
                  <Label htmlFor="verificationCode">Verification Code</Label>
                  <Input
                    onChange={(e) =>
                      setVerificationCode(Number(e.target.value))
                    }
                    id="verificationCode"
                    type="text"
                    placeholder="Enter verification code"
                    required
                    value={verificationCode}
                  />
                </div>
                <Button type="submit">Activate</Button>
                <p>
                  Verification code expired? Didn't get the email? You can get a
                  new code <a onClick={resendVerificationEmail}>here</a>
                </p>
              </div>
            </form>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default Verify;
