import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { ThemeState, RootState } from '../../types';

const saved: string = localStorage.getItem('theme') || 'light';

const themeSlice = createSlice({
  name: 'theme',
  initialState: { mode: saved } as ThemeState,
  reducers: {
    toggleTheme(state) {
      state.mode = state.mode === 'dark' ? 'light' : 'dark';
      localStorage.setItem('theme', state.mode);
    },
    setTheme(state, action: PayloadAction<string>) {
      state.mode = action.payload;
      localStorage.setItem('theme', action.payload);
    },
  },
});

export const { toggleTheme, setTheme } = themeSlice.actions;
export const selectTheme = (state: RootState): string => state.theme.mode;
export default themeSlice.reducer;
