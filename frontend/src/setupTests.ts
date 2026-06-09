import '@testing-library/jest-dom/extend-expect';
import Adapter from '@cfaester/enzyme-adapter-react-18';
import Enzyme from 'enzyme';
import 'locales/i18n';
import 'react-app-polyfill/stable';
import './__tests__/helper.chart';
import './__tests__/MockMatchMedia';

Enzyme.configure({ adapter: new Adapter() });
