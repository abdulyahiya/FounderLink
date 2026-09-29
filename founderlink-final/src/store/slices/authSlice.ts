import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import tokenService from '../../core/tokenService';
import { AuthState, User, RootState } from '../../types';

const initialState: AuthState = {
  token: tokenService.getToken(),
  user: tokenService.getUser(),
  isAuthenticated: !!tokenService.getToken(),
};

interface SetCredentialsPayload {
  token: string;
  refreshToken?: string;
  userId: number;
  role: string;
  email: string;
  name: string;
}

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials: (state, action: PayloadAction<SetCredentialsPayload>) => {
      const { token, refreshToken, userId, role, email, name } = action.payload;
      state.token = token;
      state.user = { userId, role, email, name };
      state.isAuthenticated = true;
      tokenService.setToken(token);
      if (refreshToken) tokenService.setRefreshToken(refreshToken);
      tokenService.setUser({ userId, role, email, name });
    },
    logout: (state) => {
      state.token = null;
      state.user = null;
      state.isAuthenticated = false;
      tokenService.clearAll();
    },
  },
});

export const { setCredentials, logout } = authSlice.actions;
export const selectCurrentUser = (state: RootState): User | null => state.auth.user;
export const selectIsAuthenticated = (state: RootState): boolean => state.auth.isAuthenticated;
export default authSlice.reducer;
