import { useDispatch, useStore } from 'react-redux';
import type { AppDispatch, AppStore } from 'redux/configureStore';

export const useAppDispatch = useDispatch.withTypes<AppDispatch>();
export const useAppStore = useStore.withTypes<AppStore>();
