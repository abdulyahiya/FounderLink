import { configureStore } from '@reduxjs/toolkit';
import authReducer from './slices/authSlice';
import notificationReducer from './slices/notificationSlice';
import themeReducer from './slices/themeSlice';
import startupReducer from './slices/startupSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    notifications: notificationReducer,
    theme: themeReducer,
    startups: startupReducer,
  },
});

export type AppDispatch = typeof store.dispatch;

export default store;
