import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { getAllStartups } from '../../core/api/startupApi';
import { StartupState, PaginatedStartups, Startup, RootState } from '../../types';
import { AxiosError } from 'axios';

interface FetchStartupsParams {
  page?: number;
  size?: number;
}

export const fetchStartups = createAsyncThunk<PaginatedStartups, FetchStartupsParams | undefined>(
  'startups/fetchAll',
  async (params = {}, { rejectWithValue }) => {
    try {
      const { page = 0, size = 10 } = params;
      const res = await getAllStartups(page, size);
      return res.data;
    } catch (err) {
      const axiosError = err as AxiosError<{ message?: string }>;
      return rejectWithValue(axiosError.response?.data?.message || 'Failed to load startups');
    }
  }
);

const initialState: StartupState = {
  items: [],
  totalPages: 0,
  totalElements: 0,
  currentPage: 0,
  loading: false,
  error: null,
};

const startupSlice = createSlice({
  name: 'startups',
  initialState,
  reducers: {
    setCurrentPage: (state, action: PayloadAction<number>) => {
      state.currentPage = action.payload;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchStartups.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchStartups.fulfilled, (state, action: PayloadAction<PaginatedStartups>) => {
        state.loading = false;
        state.items = action.payload.content || [];
        state.totalPages = action.payload.totalPages || 0;
        state.totalElements = action.payload.totalElements || 0;
      })
      .addCase(fetchStartups.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload as string;
      });
  },
});

export const { setCurrentPage } = startupSlice.actions;

export const selectStartups = (state: RootState): Startup[] => state.startups.items;
export const selectStartupLoading = (state: RootState): boolean => state.startups.loading;
export const selectStartupError = (state: RootState): string | null => state.startups.error;
export const selectTotalPages = (state: RootState): number => state.startups.totalPages;
export const selectTotalElements = (state: RootState): number => state.startups.totalElements;
export const selectCurrentPage = (state: RootState): number => state.startups.currentPage;

export default startupSlice.reducer;
