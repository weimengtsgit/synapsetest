import {
  convertPriorityToLabel,
  convertLabelToPriority,
  getPriorityColor,
  getPriorityValue,
} from '../priorityUtils'

describe('priorityUtils', () => {
  describe('convertPriorityToLabel', () => {
    it('should convert priority 10 to P0', () => {
      expect(convertPriorityToLabel(10)).toBe('P0')
    })

    it('should convert priority 9 to P0', () => {
      expect(convertPriorityToLabel(9)).toBe('P0')
    })

    it('should convert priority 8 to P1', () => {
      expect(convertPriorityToLabel(8)).toBe('P1')
    })

    it('should convert priority 7 to P1', () => {
      expect(convertPriorityToLabel(7)).toBe('P1')
    })

    it('should convert priority 5 to P2', () => {
      expect(convertPriorityToLabel(5)).toBe('P2')
    })

    it('should convert priority 4 to P2', () => {
      expect(convertPriorityToLabel(4)).toBe('P2')
    })

    it('should convert priority 3 to P3', () => {
      expect(convertPriorityToLabel(3)).toBe('P3')
    })

    it('should convert priority 0 to P3', () => {
      expect(convertPriorityToLabel(0)).toBe('P3')
    })

    it('should return P2 for undefined', () => {
      expect(convertPriorityToLabel(undefined)).toBe('P2')
    })

    it('should return P2 for null', () => {
      expect(convertPriorityToLabel(null)).toBe('P2')
    })
  })

  describe('convertLabelToPriority', () => {
    it('should convert P0 to 10', () => {
      expect(convertLabelToPriority('P0')).toBe(10)
    })

    it('should convert p0 to 10 (case insensitive)', () => {
      expect(convertLabelToPriority('p0')).toBe(10)
    })

    it('should convert P1 to 8', () => {
      expect(convertLabelToPriority('P1')).toBe(8)
    })

    it('should convert P2 to 5', () => {
      expect(convertLabelToPriority('P2')).toBe(5)
    })

    it('should convert P3 to 3', () => {
      expect(convertLabelToPriority('P3')).toBe(3)
    })

    it('should return 5 for undefined', () => {
      expect(convertLabelToPriority(undefined)).toBe(5)
    })

    it('should return 5 for null', () => {
      expect(convertLabelToPriority(null)).toBe(5)
    })

    it('should parse numeric strings', () => {
      expect(convertLabelToPriority('8')).toBe(8)
    })

    it('should return 5 for invalid input', () => {
      expect(convertLabelToPriority('invalid')).toBe(5)
    })
  })

  describe('getPriorityColor', () => {
    it('should return red for P0', () => {
      expect(getPriorityColor('P0')).toBe('red')
    })

    it('should return red for priority 10', () => {
      expect(getPriorityColor(10)).toBe('red')
    })

    it('should return orange for P1', () => {
      expect(getPriorityColor('P1')).toBe('orange')
    })

    it('should return orange for priority 8', () => {
      expect(getPriorityColor(8)).toBe('orange')
    })

    it('should return blue for P2', () => {
      expect(getPriorityColor('P2')).toBe('blue')
    })

    it('should return blue for priority 5', () => {
      expect(getPriorityColor(5)).toBe('blue')
    })

    it('should return default for P3', () => {
      expect(getPriorityColor('P3')).toBe('default')
    })

    it('should return default for priority 3', () => {
      expect(getPriorityColor(3)).toBe('default')
    })

    it('should return default for undefined', () => {
      expect(getPriorityColor(undefined)).toBe('default')
    })
  })

  describe('getPriorityValue', () => {
    it('should return the number as is when input is number', () => {
      expect(getPriorityValue(10)).toBe(10)
      expect(getPriorityValue(8)).toBe(8)
      expect(getPriorityValue(5)).toBe(5)
    })

    it('should convert label to number when input is string', () => {
      expect(getPriorityValue('P0')).toBe(10)
      expect(getPriorityValue('P1')).toBe(8)
      expect(getPriorityValue('P2')).toBe(5)
      expect(getPriorityValue('P3')).toBe(3)
    })

    it('should return 5 for undefined', () => {
      expect(getPriorityValue(undefined)).toBe(5)
    })
  })

  describe('Integration test - Round trip conversion', () => {
    it('should maintain consistency in round trip conversion', () => {
      // Number -> Label -> Number
      expect(convertLabelToPriority(convertPriorityToLabel(10))).toBe(10)
      expect(convertLabelToPriority(convertPriorityToLabel(8))).toBe(8)
      expect(convertLabelToPriority(convertPriorityToLabel(5))).toBe(5)
      expect(convertLabelToPriority(convertPriorityToLabel(3))).toBe(3)

      // Label -> Number -> Label
      expect(convertPriorityToLabel(convertLabelToPriority('P0'))).toBe('P0')
      expect(convertPriorityToLabel(convertLabelToPriority('P1'))).toBe('P1')
      expect(convertPriorityToLabel(convertLabelToPriority('P2'))).toBe('P2')
      expect(convertPriorityToLabel(convertLabelToPriority('P3'))).toBe('P3')
    })
  })
})
